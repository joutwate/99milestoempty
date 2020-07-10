import {ACCESS_TOKEN} from "../config/config";

export function authHeader() {
    // return authorization header with jwt token
    const accessToken = localStorage.getItem(ACCESS_TOKEN);
    if (accessToken) {
        return {Authorization: `Bearer ${accessToken}`};
    } else {
        return {};
    }
}