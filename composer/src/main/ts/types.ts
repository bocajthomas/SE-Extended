export interface Config {
    readonly operaDownloadButton: boolean
    readonly bypassCameraRollLimit: boolean
    readonly showFirstCreatedUsername: boolean
    readonly composerLogs: boolean
    readonly customSelfDestructSnapDelay: boolean
}

export interface FriendInfo {
    readonly id: number
    readonly lastModifiedTimestamp: number
    readonly username: string
    readonly userId: string
    readonly displayName: string
    readonly bitmojiAvatarId: string
    readonly bitmojiSelfieId: string
    readonly bitmojiSceneId: string
    readonly bitmojiBackgroundId: string
    readonly friendmojis: string
    readonly friendmojiCategories: string
    readonly snapScore: number
    readonly birthday: number
    readonly addedTimestamp: number
    readonly reverseAddedTimestamp: number
    readonly serverDisplayName: string
    readonly streakLength: number
    readonly streakExpirationTimestamp: number
    readonly reverseBestFriendRanking: number
    readonly isPinnedBestFriend: number
    readonly plusBadgeVisibility: number
    readonly usernameForSorting: string
    readonly friendLinkType: number
    readonly postViewEmoji: string
    readonly businessCategory: number
}

export interface Module {
    readonly name: string
    enabled: (config: Config) => boolean
    init: () => void
}

export const modules: Module[] = []

export function defineModule<T extends Module>(module: T & Record<string, any>): T {
    modules.push(module)
    return module
}
