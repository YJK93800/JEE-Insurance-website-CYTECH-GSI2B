package com.assurcy2000.dto;

import java.time.LocalDate;

public record HistoryEntry(LocalDate date, String type, String reference, String link, String status) { }
