package com.assurcy2000.dto;

import java.time.LocalDateTime;

public record ChatLine(boolean fromClient, String content, LocalDateTime sentAt) { }
