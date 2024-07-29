/* ****************************************************************************
** 
** imparse.js
** http://imparse.org
**
** Lightweight infinite-lookahead parser generator that supports basic grammars
** defined in a JSON format.
**
*/

(function (imparse) {

  imparse.tokenize = function (grammar, s) {
    // Extract terminals from grammar.
    var terminals = [];
    for (var i = 0; i < grammar.length; i++) {
      for (var nt in grammar[i]) {
        for (var j = 0; j < grammar[i][nt].length; j++) {
          for (var con in grammar[i][nt][j]) {
            var seq = grammar[i][nt][j][con];
            for (var k = 0; k < seq.length; k++) {
              if (!(seq[k] instanceof Array)) {
                terminals.push(seq[k]);
              }
            }
          }
        }
      }
    };

    var tokens = [], row = 0, col = 0;
    while (s.length > 0) {
      while (s[0] == " " || s[0] == "\n") {
        if (s[0] == "\n") {
          row++;
          col = 0;
        } else {
          col++;
        }
        s = s.slice(1);
      }
      var m = [""], len = 0;
      for (var i = 0; i < terminals.length; i++) {
        if (terminals[i] instanceof Object && 'RegExp' in terminals[i]) {
          var c = s.match(new RegExp('^' + terminals[i]['RegExp']));
          m = (c != null && c[0].length > m[0].length) ? c : m;
        } else {
          var c = s.substr(0,terminals[i].length);
          m = (c == terminals[i]) ? [c] : m;
        }
      }
      if (m[0].length > 0) {
        s = s.slice(m[0].length);
        tokens.push({'str':m[0], 'row':row, 'col':col});
        col += m[0].length;
      } else {
        if (s.length > 0)
          console.log("Did not tokenize entire string.");
        break;
      }
    }
    return tokens;
  };

  imparse.show_tokens = function (ts) {
    var s = "", row = 0, col = 0;
    for (var i = 0; i < ts.length; i++) {
      while (row < ts[i].row) { s += "\n"; row++; col = 0; }
      while (col < ts[i].col) { s += " "; col++; }
      s += ts[i].str;
      col += ts[i].str.length;
    }
    return s;
  };

  imparse.parse_tokens = function (grammar, ts_original, nonterm) {
    // Find the appropriate produciton.
    for (var i = 0; i < grammar.length; i++) {
      if (nonterm in grammar[i]) {
        // For each option in the production.
        for (var j = 0; j < grammar[i][nonterm].length; j++) {
          var ts = ts_original, seq = grammar[i][nonterm][j];
          for (var con in seq) { // Unwrap singleton JSON object.
            var success = true, subtrees = [];
            for (var k = 0; k < seq[con].length; k++) {
              if (ts.length == 0) { // This option failed, but others may succeed.
                success = false;
                break;
              }
              // Handle each type of sequence entry that can appear in the sequence.
              var entry = seq[con][k];
              if (entry instanceof Array) {
                var result = imparse.parse_tokens(grammar, ts, entry[0]);
                if (result instanceof Array && result.length == 2) {
                  subtrees.push(result[0]);
                  ts = result[1];
                } else {
                  return result;
                }
              } else if (entry instanceof Object && 'RegExp' in entry) {
                var c = ts[0].str.match(new RegExp('^' + entry['RegExp']));
                if (c != null && c[0].length == ts[0].str.length) {
                  subtrees.push(ts[0].str);
                  ts = ts.slice(1);
                } else {
                  success = false;
                  break;
                }
              } else {
                if (ts[0].str == entry) {
                  ts = ts.slice(1);
                } else {
                  success = false;
                  break;
                }
              }
            } // for each entry in the sequence

            if (success) {
              if (con.length > 0) { 
                var o = {};
                o[con] = subtrees
                return [o, ts];
              } else { // Pass-through option with only one subtree.
                if (subtrees.length != 1)
                  return {'Error': 'Improperly defined production rule.'};
                return [subtrees[0], ts];
              }
            } // if tokens parsed with option sequence successfully

          } // unwrap JSON object for constructor and sequence
        } // for each possible sequence under the non-terminal
      } // if production is the one specified by argument
    } // for each production in grammar
  };

  imparse.parse = function (grammar, s) {
    if (grammar.length > 0) {
      for (var nonterm in grammar[0]) {
        var tokens = imparse.tokenize(grammar, s);
        var tree_tokens = imparse.parse_tokens(grammar, tokens, nonterm);
        return tree_tokens[0]; // Return only the tree.
      }
    }
    return {'Error': 'Cannot use the supplied grammar object.'};
  };

})(typeof exports !== 'undefined' ? exports : (this.imparse = {}));
