import {Role, User} from "@/service/AuthService";
import {LoginSuccess, LoginSuccessRoleEnum} from "fsearch_client";

export function roleFromDto(role: LoginSuccessRoleEnum): Role {
    if (role == "ADMIN") {
        return Role.Admin
    } else {
        return Role.User
    }
}

export function userFromDto(login: LoginSuccess): User {
    return new User(login.username, roleFromDto(login.role), login.jwt)
}