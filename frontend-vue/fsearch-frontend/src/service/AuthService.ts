import {TokenProvider} from "fsearch_client";
import {store} from "@/service/Store";

export enum Role {
    User = "USER",
    Admin = "ADMIN"
}

export class User {
    constructor(readonly username: string, readonly role: Role, readonly jwt: string) {}
}

export class AuthService implements TokenProvider {
    getToken(): string {
        return store.state.loggedInUser?.jwt ?? ""
    }
}
