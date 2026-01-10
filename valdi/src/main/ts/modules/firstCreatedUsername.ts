import { defineModule } from "../types";
import { getFriendOriginalUsername } from "../imports";
import { interceptComponent } from "../utils";

export default defineModule({
    name: "Show First Created Username",
    enabled: config => config.showFirstCreatedUsername,
    init() {
        interceptComponent(
            'common_profile/src/identity/ProfileIdentityView',
            'ProfileIdentityView',
            {
                onRender: (component: any, _args: any[], render: () => void) => {
                    if (component.viewModel) {
                        let firstCreatedUsername = getFriendOriginalUsername(component.viewModel.username);
                        if (firstCreatedUsername) {
                            if (firstCreatedUsername != component.viewModel.username) {
                                component.viewModel.username += " (" + firstCreatedUsername + ")";
                            }
                        }
                    }
                    render();
                }
            }
        )
    }
});