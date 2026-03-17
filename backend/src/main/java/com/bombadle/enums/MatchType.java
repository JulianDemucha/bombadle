package com.bombadle.enums;

/**
 * HIGHER and LOWER are made for firstAppearanceEpisode.
 * NOT_FULL_MATCH is made for Affiliations and Colors,
 * because you can have multiple of those and you can guess some of them right and some of them wrong
 */
public enum MatchType {
    MATCH,
    NOT_MATCH,
    HIGHER,
    LOWER,
    NOT_FULL_MATCH,
}