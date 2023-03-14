package data_structures;

import database.elements.SchemaElement;

public class SQLElement {
    public Block block;
    public ParseTreeNode node;

    public SQLElement(Block block, ParseTreeNode node) {
        this.block = block;
        this.node = node;
    }

    public String toString(Block block, String attribute)
    {
        String result = "";

        if (node.mappedSchemaElements.isEmpty()) return result;

        if(block.equals(this.block))
        {
            SchemaElement element = node.mappedSchemaElements.get(node.choice).schemaElement;
            result += element.relation.name + "." + element.name;
        }
        else if(this.block.outerBlock.equals(block))
        {
            if(attribute.isEmpty())
            {
                result += "block_" + this.block.blockId + "." + node.parent.function;
            }
            else
            {
                result += "block_" + this.block.blockId + "." + attribute;
            }
        }
        else
        {
            if(attribute.isEmpty())
            {
                result += "block_" + this.block.outerBlock.blockId + "." + node.parent.function;
            }
            else
            {
                result += "block_" + this.block.blockId + "." + attribute;
            }
        }

        return result;
    }

}
