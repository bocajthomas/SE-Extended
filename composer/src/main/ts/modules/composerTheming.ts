import { defineModule } from "../types";
import {dumpObject, interceptComponent} from "../utils"

export default defineModule({
    name: "Composer Theming",
    enabled: config => config.composerTheming,
    init() {
        interceptComponent(
            'composer_core/src/JSX',
            'jsx',
            {
                setAttribute({argumentsList}: { target: any, argumentsList: any }) {
                    const key = argumentsList[0];
                    if (key === "style") {
                        console.log("setAttribute", key);
                        dumpObject(argumentsList[1]);
                    }
                },
                setAttributeString({target, argumentsList}: { target: any, argumentsList: any }, thisArg) {
                    const key = argumentsList[0];
                    if (key === "backgroundColor" || key === "background" || key === "tint") {
                        argumentsList[1] = "#ffffffff";
                    }
                    let result = Reflect.apply(target, thisArg, argumentsList);
                }
            });
    },
)



