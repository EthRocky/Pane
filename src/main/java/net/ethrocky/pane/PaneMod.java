package net.ethrocky.pane;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaneMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("pane");

    @Override
    public void onInitialize() {
        LOGGER.info("Pane initialised.");
    }
}
//?}
