package com.daoninhthai.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenIntrospectionResponse {

    private boolean active;

    @JsonProperty("sub")
    private String subject;

    @JsonProperty("client_id")
    private String clientId;

    private String scope;

    private Long exp;

    @JsonProperty("token_type")
    private String tokenType;

    private String iss;

    @JsonProperty("aud")
    private List<String> audience;

}
