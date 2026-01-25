import { defineModule } from "../types";
import { interceptComponent } from "../utils";

export default defineModule({
    name: "Bypass Camera Roll Selection Limit",
    enabled: config => config.bypassCameraRollLimit,
    init() {
        interceptComponent(
            'memories_ui/src/clickhandlers/MultiSelectClickHandler',
            'MultiSelectClickHandler',
            {
                "<init>": (args: any[], superCall: () => void) => {
                    args[1].selectionLimit = 9999999;
                    superCall();
                }
            }
        )
    }
});