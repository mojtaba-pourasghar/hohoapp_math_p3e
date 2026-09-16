package com.hoohoomath.app.data;

import java.util.Arrays;
import java.util.List;

/** The 3rd-grade math textbook's table of contents: 8 chapters x 5 sections each. */
public final class Book {

    public static final class Chapter {
        public final int index;
        public final String numberFa;
        public final String title;
        public final List<String> sections;

        Chapter(int index, String numberFa, String title, String... sections) {
            this.index = index;
            this.numberFa = numberFa;
            this.title = title;
            this.sections = Arrays.asList(sections);
        }
    }

    public static final List<Chapter> CHAPTERS = Arrays.asList(
        new Chapter(0, "۱", "الگوها",
            "حلّ مسئله: الگویابی", "شمارش چندتا چندتا", "ماشین‌های ورودی ـ خروجی", "ساعت در بعدازظهر", "الگوهای متقارن"),
        new Chapter(1, "۲", "عددهای چهار رقمی",
            "حلّ مسئله: الگوسازی", "معرّفی عدد هزار", "ارزش مکانی", "ارزش پول", "تقریب عددها"),
        new Chapter(2, "۳", "عددهای کسری",
            "حلّ مسئله: رسم شکل", "کسر", "کاربرد کسر در اندازه‌گیری", "تساوی کسرها", "مقایسه‌ی کسرها"),
        new Chapter(3, "۴", "ضرب و تقسیم",
            "حلّ مسئله: روش‌های نمادین", "ضرب", "ضرب عددهای یک‌رقمی", "خاصیت‌های ضرب", "تقسیم"),
        new Chapter(4, "۵", "محیط و مساحت",
            "حلّ مسئله: زیرمسئله", "خط، نیم‌خط و پاره‌خط", "محیط", "اندازه‌ی سطح", "واحد اندازه‌گیری سطح"),
        new Chapter(5, "۶", "جمع و تفریق",
            "حلّ مسئله: مسئله‌ی ساده‌تر", "مقایسه‌ی عددها", "جمع و تفریق", "جمع در جدول ارزش مکانی", "تفریق در جدول ارزش مکانی"),
        new Chapter(6, "۷", "آمار و احتمال",
            "حلّ مسئله: حدس و آزمایش", "جدول داده‌ها", "احتمال", "نمودار دایره‌ای", "انتخاب نمودار"),
        new Chapter(7, "۸", "ضرب عددها",
            "حلّ مسئله: حذف حالت‌های نامطلوب", "ضرب در عدد ۱۰", "ضرب یک‌رقمی در چندرقمی", "محاسبه‌ی ضرب", "تقسیم با باقی‌مانده")
    );

    private Book() {}

    public static Chapter chapter(int index) {
        return CHAPTERS.get(index);
    }

    public static final int CHAPTER_COUNT = 8;
    public static final int SECTIONS_PER_CHAPTER = 5;
}
