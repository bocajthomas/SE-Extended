import { getConfig, log } from "./imports";
import { modules } from "./types";

import "./modules/operaDownloadButton";
import "./modules/firstCreatedUsername";
import "./modules/bypassCameraRollSelectionLimit";
import "./modules/selfDestructSnapDelay";


try {
    const config = getConfig();

    if (config.composerLogs) {
        ["log", "error", "warn", "info", "debug"].forEach(method => {
            console[method] = (...args: any) => log(method, Array.from(args).join(" "));
        })
    }

    modules.forEach(m => {
        if (!m.enabled(config)) {
            return
        }
        try {
            m.init();
            console.debug(`module ${m.name} initialized`);
        } catch (e) {
            console.error(`failed to initialize module ${m.name}`, e, e.stack);
        }
    });

    console.debug("modules loaded!");
} catch (e) {
    log("error", "Failed to load composer modules\n" + e + "\n" + e.stack)
}
