/*
 * rerout-java — Official Java SDK for the Rerout branded-link API.
 *
 * Copyright (c) 2026 Codecraft Solutions
 * Licensed under the MIT License — https://codecraftsolutions.co.za
 */

package co.rerout.sdk.model;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * A Smart Link routing rule. When a rule matches the incoming request the link
 * resolves to the rule's {@code targetUrl} instead of the link's default
 * destination.
 *
 * <p>Used both in the {@link Link} response and as a request item on
 * {@link CreateLinkInput} and {@link UpdateLinkInput} — the field names mirror
 * the server shape so Gson (de)serializes it directly.
 *
 * <ul>
 *   <li>{@code conditionType} is one of {@code country} / {@code device}.
 *   <li>{@code conditionOp} is one of {@code is} / {@code is_not} / {@code in}.
 *   <li>{@code conditionValue} is the value(s) to match against (for example
 *       {@code ZA}, {@code mobile}, or a comma-separated list for {@code in}).
 * </ul>
 */
public final class RoutingRule {

    @SerializedName("condition_type")
    private final String conditionType;

    @SerializedName("condition_op")
    private final String conditionOp;

    @SerializedName("condition_value")
    private final String conditionValue;

    @SerializedName("target_url")
    private final String targetUrl;

    /**
     * Creates a {@code RoutingRule}.
     *
     * @param conditionType  the attribute to match — {@code country} or {@code device}
     * @param conditionOp    the comparison operator — {@code is}, {@code is_not}, or {@code in}
     * @param conditionValue the value(s) to match against
     * @param targetUrl      the destination when the rule matches
     */
    public RoutingRule(
            String conditionType,
            String conditionOp,
            String conditionValue,
            String targetUrl) {
        this.conditionType = conditionType;
        this.conditionOp = conditionOp;
        this.conditionValue = conditionValue;
        this.targetUrl = targetUrl;
    }

    /** {@return the attribute to match — {@code country} or {@code device}} */
    public String getConditionType() {
        return conditionType;
    }

    /** {@return the comparison operator — {@code is}, {@code is_not}, or {@code in}} */
    public String getConditionOp() {
        return conditionOp;
    }

    /** {@return the value(s) to match against} */
    public String getConditionValue() {
        return conditionValue;
    }

    /** {@return the destination URL when the rule matches} */
    public String getTargetUrl() {
        return targetUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoutingRule)) {
            return false;
        }
        RoutingRule other = (RoutingRule) o;
        return Objects.equals(conditionType, other.conditionType)
                && Objects.equals(conditionOp, other.conditionOp)
                && Objects.equals(conditionValue, other.conditionValue)
                && Objects.equals(targetUrl, other.targetUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionType, conditionOp, conditionValue, targetUrl);
    }

    @Override
    public String toString() {
        return "RoutingRule{conditionType=" + conditionType
                + ", conditionOp=" + conditionOp
                + ", conditionValue=" + conditionValue
                + ", targetUrl=" + targetUrl + "}";
    }
}
