/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.expression.std;

import com.foundationdb.qp.operator.QueryContext;
import com.foundationdb.server.error.InvalidArgumentTypeException;
import com.foundationdb.server.error.WrongExpressionArityException;
import com.foundationdb.server.expression.*;
import com.foundationdb.server.service.functions.Scalar;
import com.foundationdb.server.types.AkType;
import com.foundationdb.server.types.NullValueSource;
import com.foundationdb.server.types.ValueSource;
import com.foundationdb.sql.StandardException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class CeilFloorExpression extends AbstractUnaryExpression {
    @Scalar("floor")
    public static final ExpressionComposer FLOOR_COMPOSER = new InternalComposer(CeilFloorName.FLOOR);
    
    @Scalar({"ceil", "ceiling"})
    public static final ExpressionComposer CEIL_COMPOSER = new InternalComposer(CeilFloorName.CEIL);

    public static enum CeilFloorName
    {
        FLOOR, CEIL;
    }
    
    private final CeilFloorName name;

    private static class InternalComposer extends UnaryComposer
    {
        private final CeilFloorName name;

        public InternalComposer(CeilFloorName funcName)
        {
            this.name = funcName;
        }

        @Override
        public ExpressionType composeType(TypesList argumentTypes) throws StandardException
        {
            int argc = argumentTypes.size();
            if (argc != 1)
                throw new WrongExpressionArityException(1, argc);
            
            AkType firstAkType = argumentTypes.get(0).getType();

            if (firstAkType == AkType.VARCHAR || firstAkType == AkType.UNSUPPORTED)
            {
                argumentTypes.setType(0, AkType.DOUBLE);
            }
                        
            return argumentTypes.get(0);
        }

        @Override
        protected Expression compose(Expression argument, ExpressionType argType, ExpressionType resultType)
        {
            return new CeilFloorExpression(argument, name);
        }
    }

    private static class InnerEvaluation extends AbstractUnaryExpressionEvaluation
    {

        private final CeilFloorName name;

        public InnerEvaluation(ExpressionEvaluation eval, CeilFloorName funcName)
        {
            super(eval);
            this.name = funcName;
        }

        @Override
        public ValueSource eval()
        {
            ValueSource firstOperand = operand();
            if (firstOperand.isNull())
                return NullValueSource.only();

            AkType operandType = firstOperand.getConversionType();

            switch (operandType)
            {
                // For any integer type, ROUND/FLOOR/CEIL just return the same value
                case INT: 
                case LONG: 
                case U_INT: 
                case U_BIGINT:
                    valueHolder().copyFrom(firstOperand); 
                    break;
                // Math.floor/ceil only with doubles, so we split FLOAT/DOUBLE to be safe with casting
                case DOUBLE:
                    double dInput = firstOperand.getDouble();
                    double finalDValue = (name == CeilFloorName.FLOOR) ? Math.floor(dInput) : Math.ceil(dInput);
                    valueHolder().putDouble(finalDValue); 
                    break;
                case U_DOUBLE:
                    double unsignedDInput = firstOperand.getUDouble();
                    double finalUnsignedDValue = (name == CeilFloorName.FLOOR) ? Math.floor(unsignedDInput) : Math.ceil(unsignedDInput);
                    valueHolder().putUDouble(finalUnsignedDValue); 
                    break;                    
                case FLOAT:
                    float fInput = firstOperand.getFloat();
                    float finalFValue = (float) ((name == CeilFloorName.FLOOR) ? Math.floor(fInput) : Math.ceil(fInput));
                    valueHolder().putFloat(finalFValue);
                    break;
                case U_FLOAT:
                    float unsignedFInput = firstOperand.getUFloat();
                    float finalUnsignedFValue = (float) ((name == CeilFloorName.FLOOR) ? Math.floor(unsignedFInput) : Math.ceil(unsignedFInput));
                    valueHolder().putUFloat(finalUnsignedFValue);
                    break;
                case DECIMAL:
                    // NOTE: BigDecimal equality requires .compareTo(); using .equals() checks if scale is equal as well
                    // In IT, this returns an integral value since its scale is zero.
                    
                    BigDecimal decInput = firstOperand.getDecimal();
                    BigDecimal finalDecValue = (name == CeilFloorName.FLOOR) ? 
                            decInput.setScale(0, RoundingMode.FLOOR) : 
                            decInput.setScale(0, RoundingMode.CEILING);
                    valueHolder().putDecimal(finalDecValue);
                    break;
                default:
                    QueryContext context = queryContext();
                    if (context != null)
                        context.warnClient(new InvalidArgumentTypeException(name.name() + operandType.name()));
                    return NullValueSource.only();
            }
            return valueHolder();
        }
    }
    
    protected CeilFloorExpression(Expression operand, CeilFloorName funcName)
    {
        super(operand.valueType(), operand);
        this.name = funcName;
    }
    
    @Override
    public String name()
    {
        return name.name();
    }

    @Override
    public ExpressionEvaluation evaluation()
    {
        return new InnerEvaluation(this.operandEvaluation(), name);
    }
}