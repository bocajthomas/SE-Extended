import { defineModule } from "../types";
import { interceptComponent } from "../utils";

export default defineModule({
    name: "Self Destruct Snap Delay",
    enabled: config => config.customSelfDestructSnapDelay,
    init() {
        interceptComponent(
            'snap_editor_timer_tool/src/TimerPickerView',
            'TimerPickerView',
            {
                "<init>": (args: any[], superCall: () => void) => {
                    if (args[1].options[0] == 30) {
                        args[1].style = 0; // seconds format

                        args[1].options = [
                            5, // 5 seconds
                            10, // 10 seconds
                            20, // 20 seconds
                            30, // 30 seconds
                            60, // 1 minute
                            120, // 2 minutes
                            180, // 3 minutes
                            240, // 4 minutes
                            300, // 5 minutes
                            600, // 10 minutes
                            900, // 15 minutes
                            1200, // 20 minutes
                            1800, // 30 minutes
                            3600, // 1 hour
                            7200, // 2 hours
                            10800, // 3 hours
                            14400, // 4 hours
                            21600, // 6 hours
                            28800, // 8 hours
                            43200, // 12 hours
                            86400, // 1 day
                            172800, // 2 days
                        ]
                    }

                    superCall();
                }
            }
        )
    }
});