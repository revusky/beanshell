package bsh;

import bsh.congo.parser.Node;
import bsh.congo.tree.*;

public class TreeConverter extends Node.Visitor {
    private Node root;
    private bsh.Node legacyRoot, currentLegacyNode;

    static public bsh.Node convert(Node root) {
        TreeConverter converter = new TreeConverter(root);
        converter.visit(root);
        return converter.legacyRoot;
    }

    TreeConverter(Node root) {this.root = root;}

    void visit(WhileStatement ws) {
        BSHWhileStatement legacyWs = new BSHWhileStatement(bsh.ParserTreeConstants.JJTWHILESTATEMENT);
        legacyWs.setLineNumber(ws.getBeginLine());
        legacyWs.setText(ws.getSource());
        if (legacyRoot == null) {
            legacyRoot = legacyWs;
        }
        if (currentLegacyNode != null) {
            currentLegacyNode.add(legacyWs);
        }
        currentLegacyNode = legacyWs;
        recurse(ws);
    }
}
