# References and credits

The works this project builds on or measures itself against, and what each was used for. The other
documents cite them by the bracketed key. Where a work is a thesis, its handle at the CTU digital
repository is given; those theses are protected by the Czech Copyright Act, so this project cites
them and quotes their numbers, and holds no text of theirs.

## The method and its first implementation

- **[Buyanovsky]** Georgii Buyanovsky: *Associative coding* (in Russian), the magazine *Monitor*, 1994,
  pp. 10-22 (as cited by [Bican]); his paper *Research method of pseudostochastic systems* (English, undated,
  distributed as `AC_ST_EN.DOC`), and the reference compressor `AC.C` with the archiver ACB 1.02c to 2.00c,
  from <http://ctxmodel.net/files/ACB.rar>. The `acb` coder (`-tc acb`) implements his method
  from the paper and from a reading of `AC.C`, which carries no licence and is not copied
  (`ALGORITHM.md` section 7). The name ACB, *Associative Coder of Buyanovsky*, is his. His paper's coding
  of a string among the contents of the funnel of posthistory is what `acbx` does a byte at a time
  (`ALGORITHM.md` 7.10 and section 9).

## Context mixing

- **[PAQ]** Matt Mahoney: the PAQ family of context-mixing compressors, and in particular *lpaq1* (2007),
  <http://mattmahoney.net/dc/#lpaq>, published under the GNU GPL; and his book *Data Compression Explained*,
  <http://mattmahoney.net/dc/dce.html>. The technique that `acbx` predicts its decisions with (`mixing`):
  counters whose rate falls with what they have seen, the mixing of predictions in the logistic domain by
  weights chosen by a context and trained on the error, and the adaptive probability map with interpolation
  over 33 steps (`ALGORITHM.md` 9.5). The code and its constants are this project's: `Logistic` computes the
  values of the logistic function it interpolates between, and nothing of lpaq1's source is used.

## The triplet forms

- **[Salomon]** David Salomon: *Data Compression: The Complete Reference*, 4th edition, Springer.
  The basic method in the form of triplets, and the flagged form (`salomon`), are described there;
  this project takes both from the way [Bican] describes them.
- **[Valach]** Michal Valach: *Optimální implementace kompresního algoritmu ACB pro knihovnu ExCom*
  (*Efficient implementation of ACB compression algorithm for ExCom library*), master's thesis, CTU FIT,
  2011, supervised by Jan Holub. <http://hdl.handle.net/10467/4076>. The ACB module of [ExCom], and the
  source of the `valach` and `salomon2` triplet forms as ExCom uses them.
- **[ExCom]** Filip Šimek, Jakub Řezníček: *ExCom, the Extensible Compression Library*, 2009 to 2010,
  <http://www.stringology.org/projects/ExCom/>. Written by Šimek in his master's thesis *Data compression
  library* (2009) and extended by Řezníček in his (2010); published under the GNU LGPL version 3, as
  [Léhar] states. It was built and run here to measure against, never copied from.

## The two theses on the longest common prefix

- **[Bican]** Jiří Bican: *Implementace vylepšení kompresní metody ACB v jazyce Java* (*Implementation of
  the ACB compression method improvements in the Java language*), master's thesis, CTU FIT, 2017,
  supervised by Radomír Polách. <http://hdl.handle.net/10467/68184>. The thesis this project accompanies:
  §1.2 the basic method, §1.3 Salomon's modifications, §3.2.1 the triplet variants, §3.3.1 the second best
  content and the longest common prefix (`lcp`), Table 5.8 the comparison that `ARCHITECTURE.md` redoes.
- **[Léhar]** Adam Léhar: *Efektivní implementace nově vyvinutých variant kompresní metody ACB pro
  knihovnu ExCom* (*Effective implementation of state-of-the-art variants of the ACB compression method for
  the ExCom library*), master's thesis, CTU FIT, 2016, supervised by Radomír Polách.
  <http://hdl.handle.net/10467/65124>. The ExCom module `acb2`. Its §3.5.1, "V1", is the same idea as
  `lcp`, implemented independently. Both assignments say the variants to study were given by the
  supervisor, Radomír Polách; neither thesis says who devised this one.

## Measured against

- **Calgary corpus:** T. C. Bell, I. H. Witten, J. G. Cleary: *Modeling for text compression*, ACM Computing
  Surveys 21(4), 1989. The 14 classic files are used (`ARCHITECTURE.md`, "Reproducing the reference rows").
- **[Mahoney]** Matt Mahoney: the *Large Text Compression Benchmark*, <http://mattmahoney.net/dc/text.html#2185>
  (the ACB entry, the page [Bican] cites). It lists ACB 2.00c on enwik8 and enwik9 only.
- **[Mahoney-DCE]** Matt Mahoney: *Data Compression Explained*, <http://mattmahoney.net/dc/dce.html>, section
  2.1.4 "Results": the compressed sizes on `calgary.tar`, a tar of the 14 files, compressed as one file. Its row
  `acb 2.00a  LZ77  1997  George Buyanovsky  778,760  18.7  18.7` (size, compression and decompression time in
  process seconds on a 2 GHz T3200 under Windows Vista) is the README's and `harness.ReferenceResults`'s `ACB 2.00a`
  row. The other rows here are the 14 files each compressed on its own, so the two are not quite alike, and the
  same table shows the difference going both ways: bzip2 -9 860,097 and gzip -9 1,022,810 on the tar against
  828,347 and 1,017,624 on the files, but LZMA (7zip 9.08a) 824,573 on the tar against 845,952 for xz -9 on the
  files. A coder with a large window gains from the tar, as ACB 2.00 would; by how much it is not measured.

## What is this project's own

- The coders `simple`, `salomon`, `salomon2`, `valach` and `lcp` implement the forms above from their
  descriptions, and `ALGORITHM.md` lists every rule and each place the source is silent or is not followed.
- `prefix` (`ALGORITHM.md` 5.6) is a variant of this project's, not of any source: [Bican]'s prefix idea with
  the rule that `AC.C` uses for its lengths.
- `acbx` (`ALGORITHM.md` section 9) is a variant of this project's: `acb`'s dictionary and funnels, the byte-wise
  walk of [Buyanovsky]'s paper, and the decisions predicted as [PAQ] predicts bits; the repeats are LZ77's
  repeated match in associative terms. The choice of its models and contexts was measured here.
- `CONTEXT_ARITHMETIC` and the range coder, the block format, the dictionary structures and the harnesses.
