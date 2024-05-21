package com.github.wekaito.backend;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CardService {

    private final String baseUrl = "https://raw.gitmirror.com/TakaOtaku/Digimon-Card-App/main/src/";

    private final CardRepo cardRepo;

    private final ChineseCardRepo chineseCardRepo;

    private final List<Card> cardCollection;

    private final Card fallbackCard = new Card(
            "1110101",
            "Fallback Card",
            "https://raw.gitmirror.com/WE-Kaito/digimon-tcg-simulator/main/frontend/src/assets/tokens/tokenCard.jpg",
            "Digimon",
            List.of("Unknown"),
            "Fallback",
            "1110101",
            List.of(new DigivolveCondition("Unknown", 0, 0)),
            null,
            "Rookie",
            List.of("Unknown"),
            0,
            0,
            1,
            "If you see this card, the actual card was not found.",
            null, null, null, null, null, null, new Restrictions(null, null, null, null), null);

    private static final Gson gson = new Gson();

    public List<Card> getCards() {
        return cardCollection;
    }

    public Card getCardByUniqueCardNumber(String uniqueCardNumber) {
        return cardCollection.stream().filter(card -> uniqueCardNumber.equals(card.uniqueCardNumber())).findFirst()
                .orElse(fallbackCard);
    }

    private final WebClient webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.toString())
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(1024 * 1024 * 10))
                    .build())
            .build();


    private final WebClient webClient2 = WebClient.builder()
            .baseUrl("https://api.digicamoe.com/api/cdb/cards/search?page=1&limit=100000")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.toString())
            .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(1024 * 1024 * 10))
                    .build())
            .build();

    @PostConstruct
    public void init() {
        fetchCards();
    }

    @Scheduled(fixedRate = 10800000) // 3 hours
    void fetchCards() {
        String responseBodyFromDigimon = webClient2.post()
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        Type listTypeInChinese = new TypeToken<FetchChineseCard>() {
        }.getType();

        FetchChineseCard fetchedChineseCards = gson.fromJson(responseBodyFromDigimon, listTypeInChinese);
        List<ChineseCard> chineseCards = new ArrayList<>();
        String[] colorsInChinese = {"红", "蓝", "黄", "绿", "黑", "白", "紫"};
        // String imgUrl = "https://dtcg-pics.moecard.cn/img/"; Jp
        String imgUrl = "https://dtcg-wechat.moecard.cn/img/card/"; // Cn

        assert Objects.requireNonNull(fetchedChineseCards).data().list() != null;

        fetchedChineseCards.data().list().forEach(card -> {

            String specialDigivolve = null;
            if (card.evo_cond() != null && !card.evo_cond().isEmpty() && !card.evo_cond().equals("-")) {
                String[] evo_conds = card.evo_cond().split("\n");
                String evo_cond = evo_conds[0];

                if (evo_conds.length > 1) {
                    specialDigivolve = evo_conds[1];
                }
                for (String cond : evo_cond.split(";")) {
                    String[] split = cond.split(":");
                    if (!Arrays.asList(colorsInChinese).contains(split[0])) {
                        specialDigivolve = "<进化> " + cond.split("~")[0] + " 费用 " + cond.split("~")[1];
                    }
                }

            }

            chineseCards.add(new ChineseCard(
                    card.serial(),
                    card.scName(),
                    imgUrl + card.images().get(0).img_path() + "~card.jpg",
                    specialDigivolve,
                    card.effect(),
                    card.evo_cover_effect(),
                    card.security_effect()
            ));
        });

        for (ChineseCard repoCard : this.chineseCardRepo.findAll()) {
            if (chineseCards.stream().noneMatch(card -> card.serial().equals(repoCard.serial()))) {
                chineseCards.add(repoCard);
            }
        }

        this.chineseCardRepo.deleteAll();
        this.chineseCardRepo.saveAll(chineseCards);

        String responseBody = webClient.get()
                .uri("assets/cardlists/PreparedDigimonCardsENG.json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        Type listType = new TypeToken<List<FetchCard>>() {
        }.getType();
        List<FetchCard> fetchedCards = gson.fromJson(responseBody, listType);
        List<Card> cards = new ArrayList<>();

        assert fetchedCards != null;
        fetchedCards.forEach(card -> {
            if (card.id().contains("BT18")) {
                return;
            }
            List<DigivolveCondition> digivolveConditions = card.digivolveCondition().stream()
                    .map(condition -> new DigivolveCondition(
                            condition.color(),
                            Integer.parseInt(condition.cost()),
                            Integer.parseInt(condition.level())
                    ))
                    .toList();

            List<String> digiTypes = Arrays.stream(card.type().split("/")).toList();
            List<String> colors = Arrays.stream(card.color().split("/")).toList();


            ChineseCard chineseCard = this.chineseCardRepo.findOneBySerial(card.cardNumber());

            cards.add(new Card(
                    card.id(),
                    (chineseCard != null) ? chineseCard.name() : card.name().english(),
                    (chineseCard != null) ? chineseCard.imgUrl() : baseUrl + card.cardImage(),
                    card.cardType(),
                    colors,
                    (card.attribute().equals("-")) ? null : card.attribute(),
                    (card.cardNumber().equals("-")) ? null : card.cardNumber(),
                    digivolveConditions,
                    (chineseCard != null) ? chineseCard.specialDigivolve() : ((card.specialDigivolve().equals("-")) ? null : card.specialDigivolve()),
                    (card.form().equals("-")) ? null : card.form(),
                    digiTypes,
                    (card.dp().equals("-")) ? null : Integer.parseInt(card.dp()),
                    (card.playCost().equals("-")) ? null : Integer.parseInt(card.playCost()),
                    (card.cardLv().equals("-")) ? null : Integer.parseInt(card.cardLv().split("\\.")[1]),
                    (chineseCard != null) ? chineseCard.effect() : ((card.effect().equals("-")) ? null : card.effect()),
                    (chineseCard != null) ? chineseCard.evo_cover_effect() : ((card.digivolveEffect().equals("-")) ? null : card.digivolveEffect()),
                    (card.aceEffect().equals("-")) ? null : card.aceEffect(),
                    (card.burstDigivolve().equals("-")) ? null : card.burstDigivolve(),
                    (card.digiXros().equals("-")) ? null : card.digiXros(),
                    (card.dnaDigivolve().equals("-")) ? null : card.dnaDigivolve(),
                    (chineseCard != null) ? chineseCard.security_effect() : ((card.securityEffect().equals("-")) ? null : card.securityEffect()),
                    card.restrictions(),
                    card.illustrator()));
        });


        // CardRepo is a fail-safe in case the API is missing cards or shuts down
        for (Card repoCard : this.cardRepo.findAll()) {
            if (cards.stream().noneMatch(card -> card.cardNumber().equals(repoCard.cardNumber()))) {
                cards.add(repoCard);
            }
        }

        this.cardRepo.deleteAll();
        this.cardRepo.saveAll(cards);

        if (cardCollection != cards) {
            cardCollection.clear();
            cardCollection.addAll(cards);
        }
    }
}
