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
  (`ALGORITHM.md` section 7). The name ACB, *Associative Coder of Buyanovsky*, is his.

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
  (the ACB entry, the page [Bican] cites). The README credits its `ACB 2.00a` row (778,760 bytes on the 14
  Calgary files) to Mahoney's tables; which page that figure comes from is not recorded yet (`TODO.md`).

## What is this project's own

- The coders `simple`, `salomon`, `salomon2`, `valach` and `lcp` implement the forms above from their
  descriptions, and `ALGORITHM.md` lists every rule and each place the source is silent or is not followed.
- `prefix` (`ALGORITHM.md` 5.6) is a variant of this project's, not of any source: [Bican]'s prefix idea with
  the rule that `AC.C` uses for its lengths.
- `CONTEXT_ARITHMETIC` and the range coder, the block format, the dictionary structures and the harnesses.
