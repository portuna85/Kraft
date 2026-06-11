package com.kraft.lotto.feature.saved.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "saved_numbers")
public class SavedNumbersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_token", nullable = false, length = 255)
    private String deviceToken;

    @Column(name = "numbers", nullable = false, length = 50)
    private String numbers;

    @Column(name = "label", length = 100)
    private String label;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    protected SavedNumbersEntity() {
    }

    public SavedNumbersEntity(String deviceToken, List<Integer> numbers, String label) {
        this.deviceToken = deviceToken;
        this.numbers = encodeNumbers(numbers);
        this.label = label;
        this.savedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public List<Integer> getNumbers() {
        return decodeNumbers(numbers);
    }

    public String getLabel() {
        return label;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    private static String encodeNumbers(List<Integer> nums) {
        List<Integer> sorted = new ArrayList<>(nums);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(sorted.get(i));
        }
        return sb.toString();
    }

    private static List<Integer> decodeNumbers(String encoded) {
        List<Integer> result = new ArrayList<>();
        for (String part : encoded.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(Integer.parseInt(trimmed));
            }
        }
        return Collections.unmodifiableList(result);
    }
}
