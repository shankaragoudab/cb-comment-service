package com.tarento.commenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CommentTreeIdentifierDTO {

  private String entityType;

  private String entityId;

  private String workflow;
}