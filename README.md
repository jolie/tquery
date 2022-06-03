<img src="https://github.com/jolie/tquery/raw/master/tquery_logo.png" width="100">

The <a href="https://github.com/jolie/tquery">Tquery</a> project is a query framework integrated in the Jolie language for the data handling/querying of Jolie trees.

Tquery is based on a [tree-based formalization](https://arxiv.org/abs/1904.11327) (language and semantics) of [MQuery](https://arxiv.org/abs/1603.09291), a sound variant of the Aggregation Framework, the query language of the most popular document-oriented database: MongoDB.

Tree-shaped documents are the main format in which data flows within modern digital systems --- e.g., eHealth, the Internet-of-Things, and Edge Computing.

Tquery is particularly suited to develop real-time, ephemeral scenarios, where data shall not persist in the system.

# Installation

`jpm add @jolie/tquery`

# Import

```jolie
from @jolie.tquery.main import TQuery
```

# Operators

To illustrate the operators currently implemented in TQuery, we use the following data structure

```js
[ { "date":[ 20201128 ], "t":[ 36 ], "hr":[ 66 ] }, 
  { "date":[ 20201129 ], "t":[ 36 ], "hr":[ 65 ] }, 
  { "date":[ 20201130 ], "t":[ 37 ], "hr":[ 67 ] }, 
  //...
]
```
## Match 

The purpose of the match operator is to select trees in an array `a` according to a criterion, which can be
(from left to right): i) the Boolean truth, ii) the existence of a path `p`, iii) the equality between the
application of a path `p` and a given array `a`, iv) the equality between the applications of two paths `p1`
and `p2`, and the logic connectives v) negation, vi) conjunction, and vii) disjunction.

For example, the invocation below selects (in `resp`) all elements whose `date` corresponds to either `20201128`, `20201129` or `20201130`.

```jolie
match@Tquery({ 
 data << _tmp
 query.or << { 
  left <<   { equal << { path = "date" data = 20201128 } }
  right.or << { 
   left <<  { equal << { path = "date" data = 20201129 } }
   right << { equal << { path = "date" data = 20201130 } }
}}})( resp )
```

