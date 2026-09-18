package com.hoohoomath.app.data;

import static com.hoohoomath.app.data.PersianDigits.fa;

/**
 * Procedurally generates practice/worksheet/exam questions for any chapter, section,
 * index and level using a deterministic (seeded) pseudo-random formula, so the same
 * (chapter, section, index, level) always yields the same question. Ported 1:1 from
 * the approved design prototype's item()/v() logic.
 */
public final class QuestionGenerator {

    private QuestionGenerator() {}

    /**
     * Deterministic pseudo-random integer in [lo, hi] from the question index i and a seed k.
     *
     * The first version was a plain linear formula, and for some (k, span) pairs the i term
     * cancelled out entirely — every question in the set came out identical, which is exactly what
     * a child saw in the practice sets. This mixes the bits properly, so consecutive questions
     * always differ while staying perfectly repeatable.
     */
    private static int seeded(int i, int k, int lo, int hi) {
        int span = hi - lo + 1;
        if (span <= 0) return lo;
        int h = (i + 1) * 0x9E3779B1 ^ (k + 1) * 0x85EBCA77;
        h ^= h >>> 15;
        h *= 0x2545F491;
        h ^= h >>> 13;
        return lo + (h >>> 1) % span;
    }

    public static QuestionItem item(int ch, int sec, int i, int lv) {
        final int lvFinal = lv;
        SeedFn V = (k, lo, hi) -> seeded(i, k + lvFinal * 5, lo, hi);
        int m = new int[]{1, 2, 4}[lv];

        String q;
        long a;
        String hint;

        switch (ch) {
            case 0: { // الگوها
                if (sec == 0) {
                    int s = new int[]{2, 3, 5}[lv];
                    int b = V.get(1, 2, 9);
                    q = "الگو را ادامه بده: " + fa(b) + " ، " + fa(b + s) + " ، " + fa(b + 2 * s) + " ، ⬜";
                    a = b + 3L * s;
                    hint = "هر بار " + fa(s) + " اضافه می‌شود.";
                } else if (sec == 1) {
                    int s = new int[]{2, 5, 10}[lv];
                    int b = V.get(2, 1, 6) * s;
                    q = fa(s) + "تا " + fa(s) + "تا بشمار: بعد از " + fa(b) + " سه عدد جلوتر چند می‌شود؟";
                    a = b + 3L * s;
                    hint = "سه بار " + fa(s) + " اضافه کن.";
                } else if (sec == 2) {
                    int r = V.get(3, 2, 4 + lv * 3);
                    int x = V.get(4, 3, 9 + lv * 8);
                    q = "ماشین با قانون «" + fa(r) + "+»: ورودی " + fa(x) + " → خروجی چند است؟";
                    a = x + r;
                    hint = "ورودی را با " + fa(r) + " جمع کن.";
                } else if (sec == 3) {
                    int k = V.get(5, 1, 11);
                    q = "ساعت " + fa(k) + " بعدازظهر با شمارش ۲۴ساعته چه عددی است؟";
                    a = k + 12;
                    hint = "بعدازظهر ۱۲ ساعت اضافه می‌شود.";
                } else {
                    int n = V.get(6, 2, 4 + lv * 3);
                    q = "در نیمه‌ی چپ یک شکل متقارن " + fa(n) + " خانه رنگی است. کل خانه‌های رنگی چند تا می‌شود؟";
                    a = 2L * n;
                    hint = "نیمه‌ی دیگر هم همان‌قدر رنگی است.";
                }
                break;
            }
            case 1: { // عددهای چهار رقمی
                if (sec == 0) {
                    int n = V.get(1, 2, 6 + lv * 2);
                    q = "الگوی هزارتایی: ۱۰۰۰ ، ۲۰۰۰ ، ۳۰۰۰ … عدد " + fa(n) + "ام چند است؟";
                    a = n * 1000L;
                    hint = fa(n) + " تا هزار.";
                } else if (sec == 1) {
                    int a1 = V.get(2, 1, 4 + lv * 3);
                    q = "در " + fa(a1) + " هزار، چند دسته‌ی صدتایی داریم؟";
                    a = a1 * 10L;
                    hint = "هر هزار ۱۰ تا صدتایی است.";
                } else if (sec == 2) {
                    int n = V.get(3, 1000, 9000 + lv);
                    q = "در عدد " + fa(n) + " رقم صدگان چند است؟";
                    a = (n / 100) % 10;
                    hint = "از راست، رقم سوم.";
                } else if (sec == 3) {
                    int b = V.get(4, 1, 4 + lv * 2);
                    int c = V.get(5, 1, 5);
                    q = fa(b) + " اسکناس ۵۰۰ تومانی و " + fa(c) + " سکه‌ی ۱۰۰ تومانی، چند تومان می‌شود؟";
                    a = b * 500L + c * 100L;
                    hint = "اول ۵۰۰‌ها را جمع کن.";
                } else {
                    int n = V.get(6, 120, 980) * (lv + 1);
                    q = "عدد " + fa(n) + " را به نزدیک‌ترین صد تقریب بزن.";
                    a = Math.round(n / 100.0) * 100;
                    hint = "به رقم دهگان نگاه کن.";
                }
                break;
            }
            case 2: { // عددهای کسری
                if (sec == 0) {
                    int n = V.get(1, 4, 8 + lv * 2);
                    int k = V.get(2, 1, 3);
                    q = "شکلی به " + fa(n) + " قسمت مساوی تقسیم شده و " + fa(k) + " قسمت رنگی است. صورت کسر چند است؟";
                    a = k;
                    hint = "صورت = تعداد قسمت‌های رنگی.";
                } else if (sec == 1) {
                    int n = V.get(3, 3, 9);
                    int k = V.get(4, 1, 2);
                    q = "در کسر " + fa(k) + "/" + fa(n) + " مخرج چند است؟";
                    a = n;
                    hint = "مخرج عدد پایین کسر است.";
                } else if (sec == 2) {
                    int mm = V.get(5, 2, 10) * 2 * m;
                    q = "نصف " + fa(mm) + " متر چند متر است؟";
                    a = mm / 2;
                    hint = "تقسیم بر ۲.";
                } else if (sec == 3) {
                    int n = V.get(6, 2, 6) * 2;
                    q = "۱/۲ مساوی است با ⬜/" + fa(n) + " — عدد صورت چند است؟";
                    a = n / 2;
                    hint = "نصف مخرج.";
                } else {
                    int n = V.get(7, 4, 9);
                    int k = V.get(8, 1, 3);
                    int j = k + V.get(9, 1, 3);
                    q = "بین " + fa(k) + "/" + fa(n) + " و " + fa(j) + "/" + fa(n) + " کدام بزرگ‌تر است؟ صورتِ کسر بزرگ‌تر را بنویس.";
                    a = Math.max(k, j);
                    hint = "با مخرج برابر، صورت بزرگ‌تر بزرگ‌تر است.";
                }
                break;
            }
            case 3: { // ضرب و تقسیم
                int b = V.get(1, 2, 9);
                int c = V.get(2, 2, 9);
                if (sec == 0) {
                    q = fa(b) + " دسته‌ی " + fa(c) + " تایی، چند تا می‌شود؟";
                    a = (long) b * c;
                    hint = "ضرب کن.";
                } else if (sec == 1) {
                    q = fa(b) + " × " + fa(c) + " = ⬜";
                    a = (long) b * c;
                    hint = "جدول ضرب.";
                } else if (sec == 2) {
                    int d = V.get(3, 6, 9);
                    q = fa(d) + " × " + fa(c) + " = ⬜";
                    a = (long) d * c;
                    hint = "یک‌رقمی در یک‌رقمی.";
                } else if (sec == 3) {
                    q = fa(b) + " × " + fa(c) + " = " + fa(c) + " × ⬜ — عدد جای خالی چند است؟";
                    a = b;
                    hint = "جابه‌جایی ضرب.";
                } else {
                    int d = V.get(4, 2, 9);
                    int n = d * V.get(5, 2, 9);
                    q = fa(n) + " ÷ " + fa(d) + " = ⬜";
                    a = n / d;
                    hint = "چند تا " + fa(d) + " در " + fa(n) + " جا می‌شود؟";
                }
                break;
            }
            case 4: { // محیط و مساحت
                int b = V.get(1, 2, 9) * m;
                int c = V.get(2, 2, 9) * m;
                if (sec == 0) {
                    q = "مستطیلی با طول " + fa(b) + " و عرض " + fa(c) + " سانتی‌متر: محیط چند سانتی‌متر است؟";
                    a = 2L * (b + c);
                    hint = "(طول + عرض) × ۲";
                } else if (sec == 1) {
                    q = "دو پاره‌خط به طول " + fa(b) + " و " + fa(c) + " سانتی‌متر: روی هم چند سانتی‌متر؟";
                    a = b + c;
                    hint = "جمع کن.";
                } else if (sec == 2) {
                    q = "مربعی با ضلع " + fa(b) + " سانتی‌متر: محیط چند است؟";
                    a = 4L * b;
                    hint = "ضلع × ۴";
                } else if (sec == 3) {
                    q = "مستطیل " + fa(b) + " × " + fa(c) + " سانتی‌متر: مساحت چند سانتی‌متر مربع است؟";
                    a = (long) b * c;
                    hint = "طول × عرض";
                } else {
                    q = "شکلی از " + fa(b) + " ردیفِ " + fa(c) + " تایی مربع واحد ساخته شده. چند واحد سطح دارد؟";
                    a = (long) b * c;
                    hint = "ردیف‌ها را ضرب کن.";
                }
                break;
            }
            case 5: { // جمع و تفریق
                int b = V.get(1, 120, 900) * m;
                int c = V.get(2, 30, 400) * m;
                if (sec == 0) {
                    q = fa(b) + " + " + fa(c) + " = ⬜";
                    a = b + c;
                    hint = "اول صدها را جمع کن.";
                } else if (sec == 1) {
                    q = "کدام عدد بزرگ‌تر است؟ " + fa(b) + " یا " + fa(c) + " — عدد بزرگ‌تر را بنویس.";
                    a = Math.max(b, c);
                    hint = "رقم‌ها را از چپ مقایسه کن.";
                } else if (sec == 2) {
                    q = fa(b) + " − " + fa(c) + " = ⬜";
                    a = b - c;
                    hint = "از یکان شروع کن.";
                } else if (sec == 3) {
                    q = "در جدول ارزش مکانی: " + fa(b) + " + " + fa(c + 7) + " = ⬜";
                    a = b + c + 7;
                    hint = "یکان، دهگان، صدگان.";
                } else {
                    q = "در جدول ارزش مکانی: " + fa(b + c) + " − " + fa(c) + " = ⬜";
                    a = b;
                    hint = "قرض گرفتن را یادت باشد.";
                }
                break;
            }
            case 6: { // آمار و احتمال
                int b = V.get(1, 3, 12) * m;
                int c = V.get(2, 2, 9) * m;
                int e = V.get(3, 1, 8) * m;
                if (sec == 0) {
                    q = "مجموع دو عدد " + fa(2 * b) + " است و هر دو برابرند. هر عدد چند است؟";
                    a = b;
                    hint = "نصف مجموع.";
                } else if (sec == 1) {
                    q = "در جدول: " + fa(b) + " نفر فوتبال، " + fa(c) + " نفر والیبال و " + fa(e) + " نفر شنا. همه چند نفرند؟";
                    a = (long) b + c + e;
                    hint = "سه عدد را جمع کن.";
                } else if (sec == 2) {
                    q = "در کیسه " + fa(b) + " مهره‌ی قرمز و " + fa(c) + " مهره‌ی آبی است. همه‌ی مهره‌ها چند تاست؟";
                    a = b + c;
                    hint = "برای احتمال، اول کل را بشمار.";
                } else if (sec == 3) {
                    int n = V.get(4, 4, 8);
                    int k = V.get(5, 1, 3);
                    q = "نمودار دایره‌ای به " + fa(n) + " قسمت مساوی تقسیم شده و " + fa(k) + " قسمت آبی است. چند قسمت آبی نیست؟";
                    a = n - k;
                    hint = "کل منهای آبی‌ها.";
                } else {
                    q = "داده‌ها: " + fa(b) + " و " + fa(c) + " و " + fa(e) + " — بلندترین ستون نمودار چه عددی است؟";
                    a = Math.max(b, Math.max(c, e));
                    hint = "بزرگ‌ترین داده.";
                }
                break;
            }
            default: { // ch === 7: ضرب عددها
                int b = V.get(1, 2, 9);
                int c = V.get(2, 11, 40) + lv * 20;
                if (sec == 0) {
                    int n = V.get(3, 2, 9) * 5;
                    q = "کدام عدد در ۵ ضرب شود، " + fa(n) + " می‌شود؟";
                    a = n / 5;
                    hint = "تقسیم بر ۵.";
                } else if (sec == 1) {
                    q = fa(b * m) + " × ۱۰ = ⬜";
                    a = b * m * 10L;
                    hint = "یک صفر اضافه می‌شود.";
                } else if (sec == 2) {
                    q = fa(b) + " × " + fa(c) + " = ⬜";
                    a = (long) b * c;
                    hint = "یکان و دهگان را جدا ضرب کن.";
                } else if (sec == 3) {
                    q = fa(b) + " × " + fa(c + 5) + " = ⬜";
                    a = (long) b * (c + 5);
                    hint = "ضرب در جدول را بنویس.";
                } else {
                    int d = V.get(4, 3, 9);
                    int n = d * V.get(5, 2, 9) + V.get(6, 1, d - 1);
                    q = fa(n) + " ÷ " + fa(d) + " — باقی‌مانده چند است؟";
                    a = n % d;
                    hint = "تا جای ممکن تقسیم کن، بقیه باقی‌مانده است.";
                }
                break;
            }
        }

        return new QuestionItem(q, fa(a), a, hint, ch, sec);
    }

    private interface SeedFn {
        int get(int k, int lo, int hi);
    }
}
