package net.ethrocky.pane.markup;

import net.ethrocky.pane.widget.Window;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** File-driven windows: build a window from a .pml (+ optional .pss) and rebuild
 *  it whenever either file changes. Callbacks fire on a daemon watcher thread —
 *  the caller marshals to its render thread. A failed rebuild reports the error
 *  and keeps the previous window alive. */
public final class PaneMarkup {
    private PaneMarkup() {
    }

    /** One synchronous build (errors propagate), then watch until closed. */
    public static AutoCloseable watch(Path pml, Path pss, PmlContext ctx,
                                      Consumer<Window> onReload, Consumer<Exception> onError) throws IOException {
        onReload.accept(build(pml, pss, ctx));

        WatchService watcher = FileSystems.getDefault().newWatchService();
        Set<Path> dirs = new HashSet<>();
        dirs.add(pml.toAbsolutePath().getParent());
        if (pss != null) dirs.add(pss.toAbsolutePath().getParent());
        for (Path dir : dirs) {
            dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
        }

        Set<Path> watched = new HashSet<>();
        watched.add(pml.getFileName());
        if (pss != null) watched.add(pss.getFileName());

        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    WatchKey key = watcher.take();
                    boolean relevant = false;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context() instanceof Path p && watched.contains(p.getFileName())) {
                            relevant = true;
                        }
                    }
                    key.reset();
                    if (!relevant) continue;
                    // Editors fire several events per save — settle before rebuilding.
                    Thread.sleep(150);
                    while (watcher.poll() instanceof WatchKey extra) {
                        extra.pollEvents();
                        extra.reset();
                    }
                    try {
                        onReload.accept(build(pml, pss, ctx));
                    } catch (Exception e) {
                        onError.accept(e);
                    }
                }
            } catch (InterruptedException | java.nio.file.ClosedWatchServiceException ignored) {
                // closed — fall through
            }
        }, "pane-markup-watch");
        thread.setDaemon(true);
        thread.start();

        return () -> {
            thread.interrupt();
            watcher.close();
        };
    }

    /** One-shot build from files. */
    public static Window build(Path pml, Path pss, PmlContext ctx) {
        try {
            Window win = Pml.window(Files.readString(pml), ctx);
            if (pss != null && Files.exists(pss)) {
                Pss.apply(win, Pss.parse(Files.readString(pss)));
            }
            return win;
        } catch (IOException e) {
            throw new PmlException("Cannot read markup file: " + e.getMessage(), e);
        }
    }
}
