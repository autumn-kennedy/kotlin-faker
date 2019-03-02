package com.github.sergio.igwt.kfaker.provider

import com.github.sergio.igwt.kfaker.Faker
import com.github.sergio.igwt.kfaker.FakerService
import com.github.sergio.igwt.kfaker.dictionary.CategoryName

/**
 * [FakeDataProvider] implementation for [CategoryName.NAME] category.
 */
class Name internal constructor(fakerService: FakerService) : AbstractFakeDataProvider(fakerService) {
    override val categoryName = CategoryName.NAME

    val maleFirstName = resolve { fakerService.resolve(Faker, it, "male_first_name") }
    val femaleFirstName = resolve { fakerService.resolve(Faker, it, "female_first_name") }
    val firstName = resolve { fakerService.resolve(Faker, it, "first_name") }
    val lastName = resolve { fakerService.resolve(Faker, it, "last_name") }
    val prefix = resolve { fakerService.resolve(Faker, it, "prefix") }
    val suffix = resolve { fakerService.resolve(Faker, it, "suffix") }
    val name = resolve { fakerService.resolve(Faker, it, "name") }
    val nameWithMiddle = resolve { fakerService.resolve(Faker, it, "name_with_middle") }
}