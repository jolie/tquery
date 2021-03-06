<img src="https://github.com/jolie/tquery/raw/master/tquery_logo.png" width="100">

The <a href="https://github.com/jolie/tquery">Tquery</a> project is a query framework integrated in the Jolie language for the data handling/querying of Jolie trees.

Tquery is based on a [tree-based formalization](https://arxiv.org/abs/1904.11327) (language and semantics) of [MQuery](https://arxiv.org/abs/1603.09291), a sound variant of the Aggregation Framework, the query language of the most popular document-oriented database: MongoDB.

Tree-shaped documents are the main format in which data flows within modern digital systems --- e.g., eHealth, the Internet-of-Things, and Edge Computing.

Tquery is particularly suited to develop real-time, ephemeral scenarios, where data shall not persist in the system.

# Installation

`jpm add @jolie/tquery`

# Usage 

`from @jolie.tquery.main import TQuery`

