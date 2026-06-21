package org.example.diplom.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class RenameRequest {
    @JsonAlias("filename")
    private String name;
}