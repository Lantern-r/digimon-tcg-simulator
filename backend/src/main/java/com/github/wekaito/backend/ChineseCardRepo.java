package com.github.wekaito.backend;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChineseCardRepo extends MongoRepository<ChineseCard, String> {

    ChineseCard findOneBySerial(String serial);
}