package com.hiyama.anpikakuninproject.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.hiyama.anpikakuninproject.PostTest

class User (
    @JsonProperty("name") var userName:String,
    @JsonProperty("password") var password:String,
) {
    companion object {
        fun getUserInfo() : User {
            return User(
                UserInfo.userName,
                UserInfo.password
            )
        }
    }
}