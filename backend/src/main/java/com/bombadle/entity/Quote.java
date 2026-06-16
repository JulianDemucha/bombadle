package com.bombadle.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Entity
@Table(name = "quote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_card_id", nullable = false)
    private CharacterCard characterCard;

    @Column(name = "quote_beginning", nullable = false, length = 1000)
    private String quoteBeginning;

    @ElementCollection
    @CollectionTable(name = "quote_options", joinColumns = @JoinColumn(name = "quote_id"))
    @Column(name = "option_text", nullable = false)
    @Builder.Default
    private List<String> options = new ArrayList<>();

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    @PrePersist
    @PreUpdate
    private void validateAnswers() {
        if (options == null || options.isEmpty()) {
            throw new IllegalStateException("Quote must have at least one option.");
        }

        boolean hasCorrectAnswerInOptions = options.stream()
                .anyMatch(option -> option.equals(correctAnswer));

        if (!hasCorrectAnswerInOptions) {
            throw new IllegalStateException("The correct answer must be one of the provided options.");
        }

        if (options.size() != new HashSet<>(options).size()) {
            throw new IllegalStateException("Quote options must be unique.");
        }
    }
}