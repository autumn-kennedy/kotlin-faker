package io.github.serpro69.kfaker.provider

import io.github.serpro69.kfaker.*
import io.github.serpro69.kfaker.dictionary.*

/**
 * [FakeDataProvider] implementation for [CategoryName.THE_EXPANSE] category.
 */
class TheExpanse internal constructor(fakerService: FakerService) : AbstractFakeDataProvider(fakerService) {
    override val categoryName = CategoryName.THE_EXPANSE

    val characters = resolve { fakerService.resolve(Faker, it, "characters") }
    val locations = resolve { fakerService.resolve(Faker, it, "locations") }
    val ships = resolve { fakerService.resolve(Faker, it, "ships") }
    val quotes = resolve { fakerService.resolve(Faker, it, "quotes") }
}