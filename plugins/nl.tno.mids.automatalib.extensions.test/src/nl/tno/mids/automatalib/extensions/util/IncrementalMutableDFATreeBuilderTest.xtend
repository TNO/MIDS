/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.automatalib.extensions.util

import nl.tno.mids.automatalib.extensions.util.IncrementalMutableDFATreeBuilder
import java.util.Random
import net.automatalib.incremental.dfa.Acceptance
import net.automatalib.words.Alphabet
import net.automatalib.words.Word
import net.automatalib.words.WordBuilder
import net.automatalib.words.impl.GrowingMapAlphabet
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

class IncrementalMutableDFATreeBuilderTest {
    val seed = System.currentTimeMillis
    val rand = new Random(seed)

    @Test def void regularInsertion() {
        val dfaBuilder = new IncrementalMutableDFATreeBuilder(new GrowingMapAlphabet(#{"a", "b"}))
        val word = dfaBuilder.inputAlphabet.randomWord
        dfaBuilder.insert(word)
        assertEquals(true, dfaBuilder.lookup(word).toBoolean,
            "insertion of " + word + "in dfaBuilder failed using seed " + seed)
        assertEquals(true, dfaBuilder.fastDFA.accepts(word),
            "insertion of " + word + "in fastDFA failed using seed " + seed)
        val b = rand.nextInt(word.length);
        val e = b + rand.nextInt(word.length - b);
        assertEquals(word == word.subWord(b, e), dfaBuilder.lookup(word.subWord(b, e)) == Acceptance.TRUE,
            "Lookup of subword " + word.subWord(b, e) + " failed using seed " + seed)
    }

    @Test def void distinguishingWord() {
        val dfaBuilder1 = new IncrementalMutableDFATreeBuilder(new GrowingMapAlphabet(#{"a", "b", "c"}))
        val dfaBuilder2 = new IncrementalMutableDFATreeBuilder(new GrowingMapAlphabet(#{"a", "b", "c"}))
        val word1 = dfaBuilder1.inputAlphabet.randomWord
        val word2 = dfaBuilder1.inputAlphabet.randomWord
        val word3 = dfaBuilder1.inputAlphabet.randomWord

        dfaBuilder1.insert(#{word1, word3})
        dfaBuilder2.insert(#{word1, word2})
        if (word3 != word2) {
            assertEquals(word3, dfaBuilder1.findSeparatingWord(dfaBuilder2.fastDFA, dfaBuilder2.inputAlphabet, false),
                "Distinguishing word " + word3 + " was not found using seed " + seed)
            assertEquals(word2, dfaBuilder2.findSeparatingWord(dfaBuilder1.fastDFA, dfaBuilder1.inputAlphabet, false),
                "Distinguishing word " + word2 + " was not found using seed " + seed)
        }
        dfaBuilder1.insert(word2)
        assertEquals(null, dfaBuilder2.findSeparatingWord(dfaBuilder1.fastDFA, dfaBuilder1.inputAlphabet, false),
            "Distinguishing word was not inserted correctly using seed " + seed)
    }

    def <I> Word<I> getRandomWord(Alphabet<I> alphabet) {
        val wordBuilder = new WordBuilder
        val wordLength = rand.nextInt(49) + 1
        for (var i = 0; i < wordLength; i++) {
            wordBuilder.add(alphabet.getSymbol(rand.nextInt(alphabet.size)))
        }
        return wordBuilder.toWord
    }

    @Test def void insertionOutsideAlphabet() {
        val alphabet = new GrowingMapAlphabet(#{"a", "b"})
        val dfaBuilder = new IncrementalMutableDFATreeBuilder()
        alphabet.addSymbol("c")
        alphabet.addSymbol("d")
        val word = alphabet.randomWord
        dfaBuilder.insert(word)
        assertEquals(true, dfaBuilder.lookup(word).toBoolean,
            "Insertion outside alphabet in dfaBuilder failed using seed " + seed)
        assertEquals(true, dfaBuilder.fastDFA.accepts(word),
            "Insertion outside alphabet in fastDFA failed using seed " + seed)
    }
}
