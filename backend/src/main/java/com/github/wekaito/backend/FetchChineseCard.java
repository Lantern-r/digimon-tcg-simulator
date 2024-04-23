package com.github.wekaito.backend;
import com.google.gson.annotations.SerializedName;
import java.util.List;




public record FetchChineseCard 
        (
                FetchCardDataList data,
                String message,
                Boolean success
        ) { 
}

record FetchCardDataList(
        String count,
        List<FetchCardData> list
) {
}


record FetchCardData(
        String card_id,
        String card_pack,
        String serial,
        String sub_serial,
        String japName,
        String scName,
        String rarity,
        String type,
        List<String> color,
        String level,
        String cost,
        String cost_1,
        String evo_cond,
        String DP,
        String overflow,
        String grade,
        String attribute,
        List<String> card_class,
        String effect,
        String evo_cover_effect,
        String security_effect,
        String rule_text,
        String overflow_text,
        String include_info,
        String rarity$SC,
        FetchCardPackage _package,
        List<FetchCardImage> images
) {
        @SerializedName("package")
        public FetchCardPackage getPackage() {
                return _package;
        }

        @SerializedName("class")
        public List<String> getDigimonClass() {
                return card_class;
        }
}

record FetchCardImage(
        String id,
        String card_id,
        String img_rare,
        String img_path,
        String thumb_path
) {
}

record FetchCardPackage(
        String pack_id,
        String pack_prefix,
        String language
) {
}


