import { ref } from 'vue';
import * as authLib from '../lib/auth';
const auth = ref(authLib.getAuth());
export function useAuth() {
    return {
        auth,
        refresh() {
            auth.value = authLib.getAuth();
        },
        loginAsUser(name) {
            authLib.loginAsUser(name);
            auth.value = authLib.getAuth();
        },
        logoutToGuest() {
            authLib.logoutToGuest();
            auth.value = authLib.getAuth();
        },
    };
}
