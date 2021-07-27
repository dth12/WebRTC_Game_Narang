package com.exp.narang.api.request;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("RoomRegisterPostRequest")
public class RoomRegisterPostReq {
    String title;
    int maxPlayer;
    int password;
}
