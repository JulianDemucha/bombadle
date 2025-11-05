const path = '/avatar/';
const ext = '.jpg';

export const AVATAR_LIST = [
    'AVATAR_DEFAULT',
    'AVATAR_BOMBA',
    'AVATAR_GLUS',
    'AVATAR_KURVINOX',
    'AVATAR_LECINA',
    'AVATAR_JANUSZ',
    'AVATAR_SEBEK',
    'AVATAR_TORPEDA',
    'AVATAR_SKURWOL',
    'AVATAR_MKBEWE',
    'AVATAR_GALAKPIZZA',
    'AVATAR_SULTAN',
    'AVATAR_PAPA',
    'AVATAR_YETI',
    'AVATAR_USMIECH'

]

export function getAvatarUrl(avatarUrl) {
    if (AVATAR_LIST.includes(avatarUrl)) {
        return path + avatarUrl + ext;
    }
}