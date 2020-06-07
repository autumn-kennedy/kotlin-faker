package io.github.serpro69.kfaker

import io.github.serpro69.kfaker.dictionary.Category
import io.github.serpro69.kfaker.dictionary.CategoryName
import io.github.serpro69.kfaker.dictionary.Dictionary
import io.github.serpro69.kfaker.dictionary.RawExpression
import io.github.serpro69.kfaker.dictionary.getCategoryName
import io.github.serpro69.kfaker.dictionary.toLowerCase
import io.github.serpro69.kfaker.provider.Address
import io.github.serpro69.kfaker.provider.FakeDataProvider
import io.github.serpro69.kfaker.provider.Name
import java.io.InputStream
import java.util.*
import java.util.regex.Matcher
import kotlin.NoSuchElementException
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import kotlin.collections.set
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.declaredMemberProperties

/**
 * Internal class used for resolving yaml expressions into values.
 *
 * @constructor creates an instance of this [FakerService] with the default 'en' locale if [locale] is not specified.
 */
internal class FakerService @JvmOverloads internal constructor(
    internal val faker: Faker,
    locale: String = "en", random: Random
) {
    private val randomService = RandomService(random)
    private val curlyBraceRegex = Regex("""#\{(\p{L}+\.)?(.*?)\}""")
    private val numericRegex = Regex("""(#+)[^\{\s+\p{L}+]?""")
    private val letterRegex = Regex("""(\?\?+)[^\{\s+\p{L}+]?""")
    val dictionary = load(locale.replace("_", "-"))

    /**
     * @constructor creates an instance of this [FakerService] with the given [locale]
     */
    internal constructor(faker: Faker, locale: Locale, random: Random) : this(faker, locale.toLanguageTag(), random)

    private fun getDefaultFileStreams(): List<InputStream> {
        val defaultFileNames = listOf(
            "address.yml", "ancient.yml", "animal.yml", "app.yml", "appliance.yml", "aqua_teen_hunger_force.yml",
            "artist.yml", "back_to_the_future.yml", "bank.yml", "basketball.yml", "beer.yml", "bojack_horseman.yml",
            "book.yml", "bossa_nova.yml", "breaking_bad.yml", "buffy.yml", "business.yml", "cannabis.yml", "cat.yml",
            "chiquito.yml", "chuck_norris.yml", "code.yml", "coffee.yml", "coin.yml", "color.yml", "commerce.yml",
            "community.yml", "company.yml", "compass.yml", "computer.yml", "construction.yml", "cosmere.yml",
            "crypto_coin.yml", "culture_series.yml", "currency.yml", "dc_comics.yml", "demographic.yml", "dessert.yml",
            "device.yml", "dog.yml", "dota.yml", "dr_who.yml", "dragon_ball.yml", "dumb_and_dumber.yml", "dune.yml",
            "educator.yml", "elder_scrolls.yml", "electrical_components.yml", "esport.yml", "fallout.yml",
            "family_guy.yml", "file.yml", "finance.yml", "food.yml", "football.yml", "fresh_prince_of_bel_air.yml",
            "friends.yml", "funny_name.yml", "game.yml", "game_of_thrones.yml", "gender.yml", "ghostbusters.yml",
            "grateful_dead.yml", "greek_philosophers.yml", "hacker.yml", "half_life.yml", "harry_potter.yml",
            "heroes.yml", "heroes_of_the_storm.yml", "hey_arnold.yml", "hipster.yml",
            "hitchhikers_guide_to_the_galaxy.yml", "hobbit.yml", "horse.yml", "house.yml", "how_i_met_your_mother.yml",
            "id_number.yml", "industry_segments.yml", "internet.yml", "invoice.yml", "job.yml", "kpop.yml",
            "league_of_legends.yml", "lebowski.yml", "lord_of_the_rings.yml", "lorem.yml", "lovecraft.yml",
            "markdown.yml", "marketing.yml", "measurement.yml", "michael_scott.yml", "military.yml", "movie.yml",
            "music.yml", "myst.yml", "name.yml", "nation.yml", "nato_phonetic_alphabet.yml", "new_girl.yml",
            "one_piece.yml", "opera.yml", "overwatch.yml", "parks_and_rec.yml", "phish.yml", "phone_number.yml",
            "pokemon.yml", "princess_bride.yml", "programming_language.yml", "quote.yml", "rajnikanth.yml",
            "relationship.yml", "restaurant.yml", "rick_and_morty.yml", "rock_band.yml", "rupaul.yml", "science.yml",
            "seinfeld.yml", "shakespeare.yml", "silicon_valley.yml", "simpsons.yml", "slack_emoji.yml",
            "sonic_the_hedgehog.yml", "source.yml", "south_park.yml", "space.yml", "star_trek.yml", "star_wars.yml",
            "stargate.yml", "stranger_thing.yml", "stripe.yml", "subscription.yml", "super_smash_bros.yml",
            "superhero.yml", "sword_art_online.yml", "team.yml", "the_expanse.yml", "the_it_crowd.yml",
            "the_thick_of_it.yml", "twin_peaks.yml", "umphreys_mcgee.yml", "university.yml", "v_for_vendetta.yml",
            "vehicle.yml", "venture_bros.yml", "verbs.yml", "witcher.yml", "world_cup.yml", "world_of_warcraft.yml",
            "yoda.yml", "zelda.yml"
        )
        val classLoader = this.javaClass.classLoader

        return defaultFileNames.map {
            requireNotNull(classLoader.getResourceAsStream("locales/en/${it}"))
        }
    }

    private fun getLocalizedFileStream(locale: String = "en"): InputStream? {
        val classLoader = this.javaClass.classLoader

        return classLoader.getResourceAsStream("locales/$locale.yml")
    }

    /**
     * Reads values of the default 'en' locale files into this [dictionary].
     *
     * Additionally `if (locale != null && locale.isValid)`, reads the contents of the specified locale file
     * into this [dictionary] (Will overwrite all matching keys with values from specified locale file.)
     *
     * @throws IllegalArgumentException if the [locale] is invalid or locale dictionary file is not present on the classpath.
     */
    private fun load(locale: String): Dictionary {
        val defaultValues = LinkedHashMap<String, Map<String, *>>()

        getDefaultFileStreams().forEach {
            readCategory(it, "en").entries.forEach { category ->
                if (defaultValues.containsKey(category.key)) {
                    defaultValues.merge(category.key, category.value) { t, u -> t.plus(u) }
                } else defaultValues[category.key] = category.value
            }

//             todo Add `separator` category from `locales/en.yml` file
            val enYml = requireNotNull(getLocalizedFileStream("en"))

            readCategory(enYml, "en").entries.forEach { category ->
                defaultValues[category.key] = category.value
            }
        }

        /**
         * Merges [default] and [localized] categories (providers) and values, using localized value if present.
         *
         * Will also handle partially-localized categories, including partially-localized functions with secondary_keys,
         * for example:
         *
         * IF (en.category.function.secondary_key1 AND en.category.function.secondary_key2) IS PRESENT
         * AND <locale>.category.function.secondary_key1 IS PRESENT
         * AND en.category.function.another_secondary_key2 IS ABSENT
         * THEN RETURN <locale>.category.function.secondary_key1 AND en.category.function.secondary_key2
         *
         * Currently does not handle missing <locale>.category.function.secondary_key.third_key scenarios.
         */
        fun merge(default: HashMap<String, Map<String, *>>, localized: HashMap<String, Map<String, *>>) {
            localized.forEach { category ->
                default.merge(category.key, category.value) { enMap, localizedMap ->
                    /*
                     * This is a provider level access for default providers (enMap) and localized providers (localizedMap),
                     * WHERE mapKey IS provider_name: [address, name, games, etc]
                     * AND map[mapKey] (i.e. map["name") IS provider_functions: [name.first_name, name.last_name, etc]
                     *
                     * For example:
                     * enMap.key == en.faker.games // 'games' provider for 'en' locale
                     * localizedMap.key == de.faker.games // 'games' provider for 'de' locale
                     * enMap["games"] == { {...}, {...}, pokemon={names=[...],locations=[...],moves=[...]} }
                     * localizedMap["games"] == { pokemon={names=[...]} }
                     */
                    enMap.mapValuesTo(linkedMapOf()) { (k, v) ->
                        /*
                         * This is provider_functions level access for default providers (enMap).
                         * The goal here is to find-and-replace any matching functions (v) for each provider (k).
                         * But since some functions may contain secondary_key the following is needed.
                         */
                        if (v is Map<*, *> && localizedMap.containsKey(k)) {
                            // check if function has a secondary_key that is used to resolve the values
                            // if true we assume that u[k] should also be a Map because the structure of dict files should match
                            // v IS en.faker.games.<secondary_key> (i.e pokemon)
                            v.plus(localizedMap[k] as Map<*, *>)
                        } else if (localizedMap.containsKey(k)) {
                            // check if the primary_key (function_name) matches with localized provider
                            // if v is not a map, but localized key matches, then use the values for that key
                            localizedMap[k]
                        } else {
                            // else just return the original value
                            v
                        }
                    }
                }
            }
        }

        if (locale != "en") {
            val localeFileStream = getLocalizedFileStream(locale)

            if (localeFileStream == null) {
                val localeLang = locale.substringBefore("-")

                val fileStream = getLocalizedFileStream(localeLang)
                    ?: throw IllegalArgumentException("Dictionary file not found for locale values: '$locale' or '$localeLang'")

                readCategory(fileStream, localeLang).forEach { cat ->
                    when (cat.key) {
                        // 'separator' is a bit of a special case so needs to be handled separately
                        "separator" -> defaultValues[cat.key] = cat.value
                        else -> merge(defaultValues, hashMapOf(cat.key to cat.value))
                    }
                }
            } else {
                readCategory(localeFileStream, locale).forEach { cat ->
                    when (cat.key) {
                        "separator" -> defaultValues[cat.key] = cat.value
                        else -> merge(defaultValues, hashMapOf(cat.key to cat.value))
                    }
                }
            }
        }

        val categories = defaultValues.entries.toList().map {
            val value = when (it.key) {
                "separator" -> mapOf("separator" to it.value)
                else -> it.value
            }
            Category(getCategoryName(it.key), value)
        }
        return Dictionary(categories)
    }

    /**
     * Reads values from the [inputStream] for the given [locale] and returns as [LinkedHashMap]
     * where `key` represents the category name, i.e. `address`,
     * and `value` represents the [Map] of values from this category.
     */
    @Suppress("UNCHECKED_CAST")
    private fun readCategory(inputStream: InputStream, locale: String): LinkedHashMap<String, Map<String, *>> {
        val localeValues = Mapper.readValue(inputStream, Map::class.java)[locale] as Map<*, *>
        return localeValues["faker"] as LinkedHashMap<String, Map<String, *>>
    }

    /**
     * Returns [Category] instance by its [categoryName]
     */
    fun fetchCategory(categoryName: CategoryName): Category {
        return dictionary.categories.firstOrNull { it.categoryName == categoryName }
            ?: throw NoSuchElementException("Category with name '$categoryName' not found")
    }

    /**
     * Returns raw value as [RawExpression] from a given [category] fetched by its [key]
     */
    fun getRawValue(category: Category, key: String): RawExpression {
        val parameterValue = category.values[key]
            ?: throw NoSuchElementException("Parameter '$key' not found in '${category.categoryName.toLowerCase()}' category")

        return when (parameterValue) {
            is List<*> -> {
                when (val value = randomService.randomValue(parameterValue)) {
                    is List<*> -> RawExpression(randomService.randomValue(value) as String)
                    is String -> RawExpression(value)
                    is Int -> RawExpression(value.toString())
                    else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
                }
            }
            is String -> RawExpression(parameterValue)
            else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
        }
    }

    /**
     * Returns raw value as [RawExpression] from a given [category] fetched by its [key] and [secondaryKey]
     */
    fun getRawValue(category: Category, key: String, secondaryKey: String): RawExpression {
        val parameterValue = category.values[key]
            ?: throw NoSuchElementException("Parameter '$key' not found in '${category.categoryName.toLowerCase()}' category")

        return when (parameterValue) {
            is Map<*, *> -> {
                if (secondaryKey == "") {
                    val mapValues = parameterValue.values.toList()
                    when (val secondaryValue = randomService.randomValue(mapValues)) {
                        is List<*> -> RawExpression(randomService.randomValue(secondaryValue) as String)
                        is String -> RawExpression(secondaryValue)
                        is Map<*, *> -> RawExpression(secondaryValue.toString())
                        else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
                    }
                } else {
                    parameterValue[secondaryKey]?.let { secondaryValue ->
                        when (secondaryValue) {
                            is List<*> -> RawExpression(randomService.randomValue(secondaryValue) as String)
                            is String -> RawExpression(secondaryValue)
                            is Map<*, *> -> RawExpression(secondaryValue.toString())
                            else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
                        }
                    } ?: throw NoSuchElementException("Secondary key '$secondaryKey' not found.")
                }
            }
            else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
        }
    }

    /**
     * Returns raw value as [RawExpression] from a given [category] fetched by its [key], [secondaryKey], and [thirdKey]
     */
    fun getRawValue(category: Category, key: String, secondaryKey: String, thirdKey: String): RawExpression {
        val parameterValue = category.values[key]
            ?: throw NoSuchElementException("Parameter '$key' not found in '${category.categoryName.toLowerCase()}' category")

        return when (parameterValue) {
            is Map<*, *> -> {
                if (secondaryKey != "") {
                    parameterValue[secondaryKey]?.let { secondaryValue ->
                        when (secondaryValue) {
                            is Map<*, *> -> {
                                if (thirdKey == "") {
                                    val mapValues = secondaryValue.values.toList()
                                    when (val thirdValue = randomService.randomValue(mapValues)) {
                                        is List<*> -> RawExpression(randomService.randomValue(thirdValue) as String)
                                        is String -> RawExpression(thirdValue)
                                        else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
                                    }
                                } else {
                                    secondaryValue[thirdKey]?.let { thirdValue ->
                                        when (thirdValue) {
                                            is List<*> -> RawExpression(randomService.randomValue(thirdValue) as String)
                                            is String -> RawExpression(thirdValue)
                                            else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
                                        }
                                    } ?: throw NoSuchElementException("Third key '$thirdKey' not found.")
                                }
                            }
                            else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
                        }
                    } ?: throw NoSuchElementException("Secondary key '$secondaryKey' not found.")
                } else {
                    throw IllegalArgumentException("Secondary key can not be empty string.")
                }
            }
            else -> throw UnsupportedOperationException("Unsupported type of raw value: ${parameterValue::class.simpleName}")
        }
    }

    // TODO: 3/18/2019 remove as it's unused
    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun resolveExpressionWithNumerals(rawValue: String): String {
        return rawValue.map { if (it == '#') randomService.nextInt(10) else it }.joinToString("")
    }

    /**
     * Resolves [RawExpression] value of the [key] in this [category].
     */
    fun resolve(category: Category, key: String): String {
        val rawExpression = getRawValue(category, key)
        return resolveExpression(category, rawExpression)
    }

    /**
     * Resolves [RawExpression] value of the [key] and [secondaryKey] in this [category].
     */
    fun resolve(category: Category, key: String, secondaryKey: String): String {
        val rawExpression = getRawValue(category, key, secondaryKey)
        return resolveExpression(category, rawExpression)
    }

    /**
     * Resolves [RawExpression] value of the [key], [secondaryKey], and [thirdKey] in this [category].
     */
    fun resolve(category: Category, key: String, secondaryKey: String, thirdKey: String): String {
        val rawExpression = getRawValue(category, key, secondaryKey, thirdKey)
        return resolveExpression(category, rawExpression)
    }

    /**
     * Resolves the [rawExpression] for this [category] and returns as [String].
     *
     * For yaml expressions:
     * - `#{city_prefix}` from `en: faker: address` would be resolved to getting value from `address: city_prefix`
     * - `#{Name.first_name} from `en: faker: address` would be resolved to calling [Name.name] function.
     * - `Apt. ###` returned from `en: faker: address: secondary_address` could be resolved to `Apt. 123`
     * where `123` could be a combination of any random digits.
     * - `???` from `en: faker: restaurant: name_prefix` could be resolved to `XYZ`
     * where `XYZ` could be a combination fo any random English letters in upper-case.
     *
     * Recursive expressions are also supported:
     * - `#{Name.name}` from `en: faker: book: author` that could be resolved to `#{first_name} #{last_name}` from `en: faker: name: name`
     * will be resolved to concatenating values from `en: faker: name: first_name` and `en: faker: name: last_name` and so on until
     * the expression is exhausted to the actual value.
     */
    private tailrec fun resolveExpression(category: Category, rawExpression: RawExpression): String {
        val sb = StringBuffer()

        val resolvedExpression = when {
            curlyBraceRegex.containsMatchIn(rawExpression.value) -> {
                findMatchesAndAppendTail(rawExpression.value, sb, curlyBraceRegex) {
                    val simpleClassName = it.group(1)?.trimEnd('.')

                    val replacement = when (simpleClassName != null) {
                        true -> {
                            val providerType = getProvider(simpleClassName)
                            val propertyName = providerType.getFunctionName(it.group(2))

                            providerType.callFunction(propertyName)
                        }
                        false -> getRawValue(category, it.group(2)).value
                    }

                    it.appendReplacement(sb, replacement)
                }
            }
            numericRegex.containsMatchIn(rawExpression.value) -> rawExpression.value.numerify()
            letterRegex.containsMatchIn(rawExpression.value) -> rawExpression.value.letterify()
            else -> rawExpression.value
        }

        return if (!curlyBraceRegex.containsMatchIn(resolvedExpression) &&
            !numericRegex.containsMatchIn(resolvedExpression) &&
            !letterRegex.containsMatchIn(resolvedExpression)
        ) {
            resolvedExpression
        } else resolveExpression(category, RawExpression(resolvedExpression))
    }

    /**
     * Replaces every `#` char for this [String] receiver with a random int from 0 to 9 inclusive
     * and returns the modified [String].
     */
    private fun String.numerify(): String {
        return this.map { if (it == '#') randomService.nextInt(10).toString() else "$it" }.joinToString("")
    }

    /**
     * Replaces every `?` char for this [String] receiver with a random upper-case letter from the English alphabet
     * and returns the modified [String].
     */
    private fun String.letterify(): String {
        return this.map {
            if (it == '?') randomService.nextLetter(upper = true).toString() else "$it"
        }.joinToString("")
    }

    /**
     * Calls the property of this [FakeDataProvider] receiver and returns the result as [String].
     *
     * @param T instance of [FakeDataProvider]
     * @param kFunction the [KFunction] of [T]
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : FakeDataProvider> T.callFunction(kFunction: KFunction<*>): String {
        return kFunction.call(this) as String
    }

    /**
     * Gets the [KFunction] of this [FakeDataProvider] receiver from the [rawString].
     *
     * Examples:
     *
     * Yaml expression in the form of `Name.first_name` would be translated to [Name.firstName] function.
     *
     * Yaml expression in the form of `Address.country` would be translated to [Address.country] function.
     *
     * @param T instance of [FakeDataProvider]
     */
    private fun <T : FakeDataProvider> T.getFunctionName(rawString: String): KFunction<*> {
        val propertyName = rawString.split("_").mapIndexed { i: Int, s: String ->
            if (i == 0) s else s.substring(0, 1).toUpperCase() + s.substring(1)
        }.joinToString("")

        return this::class.declaredFunctions.first { it.name == propertyName }
    }

    /**
     * Returns an instance of [FakeDataProvider] fetched by it's [simpleClassName] (case-insensitive).
     */
    private fun getProvider(simpleClassName: String): FakeDataProvider {
        val kProp = faker::class.declaredMemberProperties.first {
            it.name.toLowerCase() == simpleClassName.toLowerCase()
        }

        return kProp.call(faker) as FakeDataProvider
    }

    private fun findMatchesAndAppendTail(
        string: String,
        stringBuffer: StringBuffer,
        regex: Regex,
        invoke: (Matcher) -> Unit
    ): String {
        val matcher = regex.toPattern().matcher(string)

        while (matcher.find()) invoke(matcher)

        matcher.appendTail(stringBuffer)
        return stringBuffer.toString()
    }
}
