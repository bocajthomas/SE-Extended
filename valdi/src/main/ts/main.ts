import { getConfig, log, setEvalFunction } from "./imports";
import { modules } from "./types";

import "./modules/operaDownloadButton";
import "./modules/firstCreatedUsername";
import "./modules/bypassCameraRollSelectionLimit";
import "./modules/selfDestructSnapDelay";


try {
    const config = getConfig();

    if (config.valdiLogs) {
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

    setEvalFunction((code, callback) => {
        try {
            callback(eval(code)?.toString());
        } catch (e) {
            callback(e)
        }
    })

    console.debug("modules loaded!");
} catch (e) {
    log("error", "Failed to load valdi modules\n" + e + "\n" + e.stack)
}
