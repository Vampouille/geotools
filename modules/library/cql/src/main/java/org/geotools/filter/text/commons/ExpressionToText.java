/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2006-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.filter.text.commons;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.temporal.Period;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * This class is responsible to convert an expression to a CQL/ECQL valid expression.
 * 
 * <p>
 * Warning: This component is not published. It is part of module implementation. 
 * Client module should not use this feature.
 * </p>
 * 
 * @author Mauricio Pazos
 *
 *
 * @source $URL$
 */
public class ExpressionToText implements ExpressionVisitor {
	
    static private  StringBuilder asStringBuilder( Object extraData){
        if( extraData instanceof StringBuilder){
            return (StringBuilder) extraData;
        }
        return new StringBuilder();
    }
    /**
     * Uses the format <code>yyyy-MM-dd'T'HH:mm:ss'[+|-]##:##'</code> for
     * output the provided date.
     * @param date
     * @param output
     * @return output 
     */
    public StringBuilder dateToText( Date date, StringBuilder output ){
        final DateFormat formatter;

        // If the Date has millisecond resolution, print the millis.
        if (date.getTime() % 1000 == 0) {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
        } else {
            formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");
        }

        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String text = formatter.format(date);
		
		// GMT is not part of CQL syntax so it is removed 
		text = text.replace("GMT", "");

		output.append( text );        

		return output;
    }
    
	
	/* (non-Javadoc)
	 * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.NilExpression, java.lang.Object)
	 */
	@Override
	public Object visit(NilExpression expression, Object extraData) {

		StringBuilder output = asStringBuilder(extraData);
		output.append("\"\"");

		return output;
	}

	/* (non-Javadoc)
	 * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Add, java.lang.Object)
	 */
	@Override
	public Object visit(Add expression, Object extraData) {

        StringBuilder output = asStringBuilder(extraData);        
        expression.getExpression1().accept(this, output );
        output.append( " + " );
        expression.getExpression2().accept(this, output );
        
        return output;
	}

	/* (non-Javadoc)
	 * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Divide, java.lang.Object)
	 */
	@Override
	public Object visit(Divide expression, Object extraData) {

        StringBuilder output = asStringBuilder(extraData);        
        expression.getExpression1().accept(this, output );
        output.append( " / " );
        expression.getExpression2().accept(this, output );
        
        return output;
	}

	/* (non-Javadoc)
	 * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Function, java.lang.Object)
	 */
	@Override
	public Object visit(Function function, Object extraData) {

        StringBuilder output = asStringBuilder(extraData);        
        output.append( function.getName() );
        output.append( "(" );
        List<Expression> parameters = function.getParameters();

        if( parameters != null ){
            for( Iterator<Expression> i=parameters.iterator(); i.hasNext(); ){
                Expression argument = i.next();
                argument.accept(this, output );
                if( i.hasNext() ){
                    output.append(",");
                }
            }
        }
        output.append( ")" );        
        return output;
	}

	/* (non-Javadoc)
	 * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Literal, java.lang.Object)
	 */
	@Override
	public Object visit(Literal expression, Object extraData) {
        StringBuilder output = asStringBuilder(extraData);
        
        Object literal = expression.getValue();
        if (literal instanceof Geometry) {
            Geometry geometry = (Geometry) literal;
            WKTWriter writer = new WKTWriter();
            String wkt = writer.write( geometry );
            output.append( wkt );
        }
        else if( literal instanceof Number ){
                // don't convert to string
                output.append( literal );
        }
        else if (literal instanceof Date ){
            return dateToText( (Date) literal, output );
        }
        else if (literal instanceof Period){

            Period period = (Period) literal;
            
            output = dateToText( period.getBeginning().getPosition().getDate(), output );
            output.append("/");
    		output = dateToText( period.getEnding().getPosition().getDate(), output );
    		
    		return output;
        }
        else {
            String escaped = literal.toString().replaceAll("'", "''");
            output.append("'" + escaped + "'");
        }
        return output;
	}
	

	/* (non-Javadoc)
	 * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Multiply, java.lang.Object)
	 */
	@Override
	public Object visit(Multiply expression, Object extraData) {

        StringBuilder output = asStringBuilder(extraData);        
        expression.getExpression1().accept(this, output );
        output.append( " * " );
        expression.getExpression2().accept(this, output );
        
        return output;
	}

	/* (non-Javadoc)
	 * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.PropertyName, java.lang.Object)
	 */
	@Override
	public Object visit(PropertyName expression, Object extraData) {

        StringBuilder output = asStringBuilder(extraData);
        if(propertyNeedsDelimiters(expression)) {
            output.append('"');
            output.append( expression.getPropertyName().replace("\"", "\"\"") );
            output.append('"');
        } else {
            output.append( expression.getPropertyName() );
        }
        
        return output;
	}
    
    // Pattern for an identifier which does not require delimiting unless it's 
    // a reserved word
    private static final Pattern SIMPLE_IDENTIFIER = 
            Pattern.compile("[a-zA-Z][_a-zA-Z0-9]*");
    // Words reserved by the language which can not be used as identifiers
    // unless delimited
    private static final Set<String> RESERVED_WORDS;
    static {
        Set<String> reservedWords = 
                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        reservedWords.addAll(Arrays.asList("NOT", "AND", "OR", "LIKE", "IS", 
                "NULL", "EXISTS", "DOES-NOT-EXIST", "DURING", "AFTER", "BEFORE",
                "IN", "INCLUDE", "EXCLUDE", "TRUE", "FALSE", "EQUALS", 
                "DISJOINT", "INTERSECTS", "TOUCHES", "CROSSES", "WITHIN", 
                "CONTAINS", "OVERLAPS", "RELATE", "DWITHIN", "BEYOND", "POINT", 
                "LINESTRING", "POLYGON", "MULTIPOINT", "MULTILINESTRING", 
                "MULTIPOLYGON", "GEOMETRYCOLLECTION"));
        RESERVED_WORDS = Collections.unmodifiableSet(reservedWords);
    }
    
    protected boolean propertyNeedsDelimiters(PropertyName name) {
        if(!SIMPLE_IDENTIFIER.matcher(name.getPropertyName()).matches()) { 
            return true;
        }
        return RESERVED_WORDS.contains(name.getPropertyName());
    }


	/* (non-Javadoc)
	 * @see org.opengis.filter.expression.ExpressionVisitor#visit(org.opengis.filter.expression.Subtract, java.lang.Object)
	 */
	@Override
	public Object visit(Subtract expression, Object extraData) {
		
        StringBuilder output = asStringBuilder(extraData);        
        expression.getExpression1().accept(this, output );
        output.append( " - " );
        expression.getExpression2().accept(this, output );
        
        return output;
	}

}
