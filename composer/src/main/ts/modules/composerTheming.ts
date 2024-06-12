import { assetCatalog, jsx, style } from "../composer";
import { defineModule } from "../types";
import { interceptComponent } from "../utils"
// Composer theme test
export default defineModule({
  name: "Composertheming",
  init() {
    interceptComponent(
      proxyProperty(require('composer_core/src/JSX').jsx, "setAttribute", (target, thisArg, argumentsList) => {
        const key = argumentsList[0];
        if (key == "style") {
          console.log("setAttribute", key);
          dumpObject(argumentsList[1]);
        }
        return Reflect.apply(target, thisArg, argumentsList);
      })
      proxyProperty(require('composer_core/src/JSX').jsx, "setAttributeString", (target, thisArg, argumentsList) => {
      const key = argumentsList[0];
      if (key == "backgroundColor") {
        argumentsList[1] = "#ffffffff";
      }
      if (key == "background") {
        argumentsList[1] = "#ffffffff";
      }
      if (key == "tint") {
        argumentsList[1] = "#ffffffff";
      }
      return Reflect.apply(target, thisArg, argumentsList);
    })
  }
})
