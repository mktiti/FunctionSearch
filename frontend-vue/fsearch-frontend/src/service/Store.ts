import Vuex from "vuex";
import Vue from "vue";
import {Role, User} from "@/service/AuthService";

Vue.use(Vuex)

export class State {
    public loggedInUser: User | null = null

    public constructor(init?: Partial<State>) {
        Object.assign(this, init)
    }
}

function stateFromLocal(): State {
    return new State({
        loggedInUser: JSON.parse(localStorage['user'] ?? null) as (User | null)
    })
}

export const store = new Vuex.Store<State>({
    state: stateFromLocal(),
    mutations: {
        login (state, user) {
            state.loggedInUser = user
            localStorage.setItem('user', JSON.stringify(user))
        }
    }, getters: {
        loggedIn: state => { return state.loggedInUser !== null },
        loggedUsername: state => { return state.loggedInUser?.username },
        userLogged: state => { return state.loggedInUser?.role == Role.User },
        adminLogged: state => { return state.loggedInUser?.role == Role.Admin },
    }
})