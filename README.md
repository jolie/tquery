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
myData = [{"y":[2020], 
  "M":[{"m":[11], 
      "D":[{"d":[27], "L":[{s:['23:33'], "e":['07:04'], "q":['poor']}]},
          {"d":[28], "L":[{"s":['21:13'], "e":['09:34'], "q":['good']}]},
          {"d":[29], "L":[{"s":['21:01'], "e":['03:12'], "q":['good']},
                      {"s":['03:36'], "e":['09:58'], "q":['good']}]},
          {"d":[30], "L":[{"s":['20:33'], "e":['01:14'], "q":['poor']},
                      {"s":['01:32'], "e":['06:15'], "q":['good']}]}
      ]}]}]
}]
```
## Match 

The purpose of the match operator is to select trees in an array `a` according to a criterion, which can be
(from left to right): i) the Boolean truth, ii) the existence of a path `p`, iii) the equality between the
application of a path `p` and a given array `a`, iv) the equality between the applications of two paths `p1`
and `p2`, and the logic connectives v) negation, vi) conjunction, and vii) disjunction.

For example, the invocation below selects (in `resp`) all elements whose `date` corresponds to either `2020` `11` `29` or `2020` `11` `30`.

```jolie
match@Tquery( { 
 data << myData
 query.and << { 
  left.equal    << { path = "y", data = 2020 }
  right.and << {
   left.equal   << { path = "M.m", data = 11 }
   right.or << {
    left.equal  << { path = "M.D.d", data = 29 }
    right.equal << { path = "M.D.d", data = 30 }
}}}})( resp )
```

## Unwind

The unwind operator unfolds the elements of an array under a given path `p`.

For example, the invocation

```jolie
unwind@Tquery( { data << myData, query = "M.D.L" } )( unwindResp )
```

unfolds the data structure along the path `M.D.L`, returning

```js
[ { "y":[ 2020 ], "M":[ {"m":[ 11 ],"D":[ { "d":[ 27 ],
    "L":[ { "s":[ '23:33' ],"e":[ '07:04' ],"q":[ 'poor' ] }] }] }] },
  { "y":[ 2020 ],"M":[ {"m":[ 11 ],"D":[ {"d":[ 28 ],
    "L":[ { "s":[ '21:13' ],"e":[ '09:34' ],"q":[ 'good' ] }] }] }] },
  { "y":[ 2020 ],"M":[ {"m":[ 11 ],"D":[ {"d":[ 29 ],
    "L":[ { "s":[ '21:01'{} ],"e":[ '03:12'{} ],"q":[ 'good'{} ] }] }] }] },
  //...
]
```

## Projects

The project operator modifies the trees in an array by projecting nodes, renaming node labels, or introducing new nodes as a value definition. A value definition is either i) a value, ii) a path, iii) an array of value definitions, iv) a selection criterion or v) a ternary expression on a criterion and two value definitions.

For example, the invocation

```jolie
 project@Tquery( { 
  data << unwindResp
  query[0] << {dstPath = "year"    value.path = "y"       }
  query[1] << {dstPath = "month"   value.path = "M.m"     }
  query[2] << {dstPath = "day"     value.path = "M.D.d"   }
  query[3] << {dstPath = "quality" value.path = "M.D.L.q" }
 })( resp )
```

respetively move and renames into their respective `dstPath`s the node `y` into `year`, the node `M.m` into `month` (same level as `year`), etc. This results into the data structure

```js
[ { "year":[ 2020 ], "month":[ 11 ],"day":[ 27 ], "quality":[ 'poor' ] },
  { "year":[ 2020 ], "month":[ 11 ],"day":[ 28 ], "quality":[ 'good' ] },
  { "year":[ 2020 ], "month":[ 11 ],"day":[ 29 ], "quality":[ 'good' ] },
  //...
]
```

## Group

## Lookup
