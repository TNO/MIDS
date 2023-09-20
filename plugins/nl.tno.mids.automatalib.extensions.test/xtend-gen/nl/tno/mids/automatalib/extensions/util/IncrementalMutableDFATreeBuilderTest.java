package nl.tno.mids.automatalib.extensions.util;

import com.google.common.base.Objects;
import java.util.Collections;
import java.util.Random;
import net.automatalib.incremental.dfa.Acceptance;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import net.automatalib.words.impl.GrowingMapAlphabet;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("all")
public class IncrementalMutableDFATreeBuilderTest {
  private final long seed = System.currentTimeMillis();
  
  private final Random rand = new Random(this.seed);
  
  @Test
  public void regularInsertion() {
    GrowingMapAlphabet<String> _growingMapAlphabet = new GrowingMapAlphabet<String>(Collections.<String>unmodifiableSet(CollectionLiterals.<String>newHashSet("a", "b")));
    final IncrementalMutableDFATreeBuilder<String> dfaBuilder = new IncrementalMutableDFATreeBuilder<String>(_growingMapAlphabet);
    final Word<String> word = this.<String>getRandomWord(dfaBuilder.getInputAlphabet());
    dfaBuilder.insert(word);
    Assertions.assertEquals(Boolean.valueOf(true), Boolean.valueOf(dfaBuilder.lookup(word).toBoolean()), 
      ((("insertion of " + word) + "in dfaBuilder failed using seed ") + Long.valueOf(this.seed)));
    Assertions.assertEquals(Boolean.valueOf(true), Boolean.valueOf(dfaBuilder.getFastDFA().accepts(word)), 
      ((("insertion of " + word) + "in fastDFA failed using seed ") + Long.valueOf(this.seed)));
    final int b = this.rand.nextInt(word.length());
    int _length = word.length();
    int _minus = (_length - b);
    int _nextInt = this.rand.nextInt(_minus);
    final int e = (b + _nextInt);
    Word<String> _subWord = word.subWord(b, e);
    boolean _equals = Objects.equal(word, _subWord);
    Acceptance _lookup = dfaBuilder.lookup(word.subWord(b, e));
    boolean _equals_1 = Objects.equal(_lookup, Acceptance.TRUE);
    Word<String> _subWord_1 = word.subWord(b, e);
    String _plus = ("Lookup of subword " + _subWord_1);
    String _plus_1 = (_plus + " failed using seed ");
    String _plus_2 = (_plus_1 + Long.valueOf(this.seed));
    Assertions.assertEquals(Boolean.valueOf(_equals), Boolean.valueOf(_equals_1), _plus_2);
  }
  
  @Test
  public void distinguishingWord() {
    GrowingMapAlphabet<String> _growingMapAlphabet = new GrowingMapAlphabet<String>(Collections.<String>unmodifiableSet(CollectionLiterals.<String>newHashSet("a", "b", "c")));
    final IncrementalMutableDFATreeBuilder<String> dfaBuilder1 = new IncrementalMutableDFATreeBuilder<String>(_growingMapAlphabet);
    GrowingMapAlphabet<String> _growingMapAlphabet_1 = new GrowingMapAlphabet<String>(Collections.<String>unmodifiableSet(CollectionLiterals.<String>newHashSet("a", "b", "c")));
    final IncrementalMutableDFATreeBuilder<String> dfaBuilder2 = new IncrementalMutableDFATreeBuilder<String>(_growingMapAlphabet_1);
    final Word<String> word1 = this.<String>getRandomWord(dfaBuilder1.getInputAlphabet());
    final Word<String> word2 = this.<String>getRandomWord(dfaBuilder1.getInputAlphabet());
    final Word<String> word3 = this.<String>getRandomWord(dfaBuilder1.getInputAlphabet());
    dfaBuilder1.insert(Collections.<Word<? extends String>>unmodifiableSet(CollectionLiterals.<Word<? extends String>>newHashSet(word1, word3)));
    dfaBuilder2.insert(Collections.<Word<? extends String>>unmodifiableSet(CollectionLiterals.<Word<? extends String>>newHashSet(word1, word2)));
    boolean _notEquals = (!Objects.equal(word3, word2));
    if (_notEquals) {
      Assertions.assertEquals(word3, dfaBuilder1.findSeparatingWord(dfaBuilder2.getFastDFA(), dfaBuilder2.getInputAlphabet(), false), 
        ((("Distinguishing word " + word3) + " was not found using seed ") + Long.valueOf(this.seed)));
      Assertions.assertEquals(word2, dfaBuilder2.findSeparatingWord(dfaBuilder1.getFastDFA(), dfaBuilder1.getInputAlphabet(), false), 
        ((("Distinguishing word " + word2) + " was not found using seed ") + Long.valueOf(this.seed)));
    }
    dfaBuilder1.insert(word2);
    Assertions.assertEquals(null, dfaBuilder2.findSeparatingWord(dfaBuilder1.getFastDFA(), dfaBuilder1.getInputAlphabet(), false), 
      ("Distinguishing word was not inserted correctly using seed " + Long.valueOf(this.seed)));
  }
  
  public <I extends Object> Word<I> getRandomWord(final Alphabet<I> alphabet) {
    final WordBuilder<I> wordBuilder = new WordBuilder<I>();
    int _nextInt = this.rand.nextInt(49);
    final int wordLength = (_nextInt + 1);
    for (int i = 0; (i < wordLength); i++) {
      wordBuilder.add(alphabet.getSymbol(this.rand.nextInt(alphabet.size())));
    }
    return wordBuilder.toWord();
  }
  
  @Test
  public void insertionOutsideAlphabet() {
    final GrowingMapAlphabet<String> alphabet = new GrowingMapAlphabet<String>(Collections.<String>unmodifiableSet(CollectionLiterals.<String>newHashSet("a", "b")));
    final IncrementalMutableDFATreeBuilder<String> dfaBuilder = new IncrementalMutableDFATreeBuilder<String>();
    alphabet.addSymbol("c");
    alphabet.addSymbol("d");
    final Word<String> word = this.<String>getRandomWord(alphabet);
    dfaBuilder.insert(word);
    Assertions.assertEquals(Boolean.valueOf(true), Boolean.valueOf(dfaBuilder.lookup(word).toBoolean()), 
      ("Insertion outside alphabet in dfaBuilder failed using seed " + Long.valueOf(this.seed)));
    Assertions.assertEquals(Boolean.valueOf(true), Boolean.valueOf(dfaBuilder.getFastDFA().accepts(word)), 
      ("Insertion outside alphabet in fastDFA failed using seed " + Long.valueOf(this.seed)));
  }
}
