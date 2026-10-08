package com.hoohoomath.app.billing;

import java.util.Arrays;
import java.util.List;

/**
 * What can be bought, as the Bazaar developer panel knows it.
 *
 * The ids here must be the ids registered in the panel, letter for letter — Bazaar answers «این
 * محصول پیدا نشد» for anything else, and that is the only sign you get. They are gathered in one
 * place so correcting them is one edit, not a search through the app.
 *
 * `title` and `price` are only what the paywall writes on the button. The real price is whatever
 * the panel says; this text is for the moment before the store's own screen opens.
 */
public final class Plans {

    public static final class Plan {
        public final String sku;
        public final String type;        // Bazaar.SUBSCRIPTION or Bazaar.ONE_OFF
        public final String title;
        public final String price;
        public final String note;

        Plan(String sku, String type, String title, String price, String note) {
            this.sku = sku;
            this.type = type;
            this.title = title;
            this.price = price;
            this.note = note;
        }
    }

    /**
     * The plans, in the order the paywall shows them.
     *
     * ⚠ These are the ids and prices to check against the panel before publishing.
     */
    public static final List<Plan> ALL = Arrays.asList(
        new Plan("hoohoo_full_1m", Bazaar.SUBSCRIPTION, "اشتراک یک‌ماهه", "", "یک ماه، همه‌ی فصل‌ها"),
        new Plan("hoohoo_full_3m", Bazaar.SUBSCRIPTION, "اشتراک سه‌ماهه", "", "یک ترم تحصیلی"),
        new Plan("hoohoo_full_1y", Bazaar.SUBSCRIPTION, "اشتراک یک‌ساله", "", "همه‌ی سال تحصیلی"),
        new Plan("hoohoo_full_forever", Bazaar.ONE_OFF, "خریدِ دائمی", "", "یک بار، برای همیشه"));

    private Plans() {}

    public static Plan bySku(String sku) {
        for (Plan plan : ALL) {
            if (plan.sku.equals(sku)) return plan;
        }
        return null;
    }
}
