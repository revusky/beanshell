package bsh.congo.scaffold;

import bsh.congo.parser.Node;
import bsh.congo.tree.*;
import bsh.BSHWhileStatement;

public class TreeConverter extends Node.Visitor {
    Node root;
    bsh.Node legacyRoot, currentLegacyNode;

    static public bsh.Node convert(Node root) {
        TreeConverter converter = new TreeConverter(root);
        converter.visit(root);
        return converter.legacyRoot;
    }

    TreeConverter(Node root) {this.root = root;}

    void visit(WhileStatement ws) {
        BSHWhileStatement legacyWs = new BSHWhileStatement();
        legacyWs.setLineNumber(ws.getBeginLine());
        legacyWs.setText(ws.getSource());
        if (legacyRoot == null) legacyRoot = legacyWs;
        currentLegacyNode = legacyWs;
        recurse(ws);
    }

}