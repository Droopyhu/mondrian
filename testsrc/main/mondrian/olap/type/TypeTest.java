/*

// This software is subject to the terms of the Common Public License

// Agreement, available at the following URL:

// http://www.opensource.org/licenses/cpl.html.

// Copyright (C) 2008-2008 Julian Hyde

// All Rights Reserved.

// You must accept the terms of that agreement to use this software.

*/

package mondrian.olap.type;



import mondrian.test.TestContext;

import mondrian.olap.*;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;


/**

 * Unit test for mondrian type facility.

 *

 * @author jhyde

 * @version $Id$

 * @since Jan 17, 2008

 */

public class TypeTest extends TestCase {

    TestContext testContext = null;
    
    protected void setUp() throws Exception {
        testContext = TestContext.instance();
    }

    public void testConversions() {

        final Connection connection = testContext.getConnection();

        Cube salesCube =
            getCubeWithName("Sales", connection.getSchema().getCubes());

        assertTrue(salesCube != null);

        Dimension customersDimension = null;

        for (Dimension dimension : salesCube.getDimensions()) {

            if (dimension.getName().equals("Customers")) {

                customersDimension = dimension;

            }
        }

        assertTrue(customersDimension != null);

        Hierarchy hierarchy = customersDimension.getHierarchy();

        Member member = hierarchy.getDefaultMember();

        Level level = member.getLevel();

        Type memberType = new MemberType(

            customersDimension, hierarchy, level, member);

        final LevelType levelType =

            new LevelType(customersDimension, hierarchy, level);

        final HierarchyType hierarchyType =

            new HierarchyType(customersDimension, hierarchy);

        final DimensionType dimensionType =

            new DimensionType(customersDimension);

        final StringType stringType = new StringType();

        final ScalarType scalarType = new ScalarType();

        final NumericType numericType = new NumericType();

        final DateTimeType dateTimeType = new DateTimeType();

        final DecimalType decimalType = new DecimalType(10, 2);

        final DecimalType integerType = new DecimalType(7, 0);

        final NullType nullType = new NullType();

        final MemberType unknownMemberType = MemberType.Unknown;

        final TupleType tupleType =

            new TupleType(

                new Type[] {memberType,  unknownMemberType});

        final SetType tupleSetType = new SetType(tupleType);

        final SetType setType = new SetType(memberType);

        final LevelType unknownLevelType = LevelType.Unknown;

        final HierarchyType unknownHierarchyType = HierarchyType.Unknown;

        final DimensionType unknownDimensionType = DimensionType.Unknown;

        final BooleanType booleanType = new BooleanType();

        Type[] types = {

            memberType,

            levelType,

            hierarchyType,

            dimensionType,

            numericType,

            dateTimeType,

            decimalType,

            integerType,

            scalarType,

            nullType,

            stringType,

            booleanType,

            tupleType,

            tupleSetType,

            setType,

            unknownDimensionType,

            unknownHierarchyType,

            unknownLevelType,

            unknownMemberType

        };



        for (Type type : types) {

            // Check that each type is assignable to itself.

            final String desc = type.toString() + ":" + type.getClass();

            assertEquals(desc, type, type.computeCommonType(type, null));



            int[] conversionCount = {0};

            assertEquals(desc, type, type.computeCommonType(type, conversionCount));

            assertEquals(0, conversionCount[0]);



            // Check that each scalar type is assignable to nullable with zero

            // conversions.

            if (type instanceof ScalarType) {

                assertEquals(type, type.computeCommonType(nullType, null));



                assertEquals(type, type.computeCommonType(nullType, conversionCount));

                assertEquals(0, conversionCount[0]);

            }

        }



        for (Type fromType : types) {

            for (Type toType : types) {

                Type type = fromType.computeCommonType(toType, null);

                Type type2 = toType.computeCommonType(fromType, null);

                final String desc =

                    "symmetric, from " + fromType + ", to " + toType;

                assertEquals(desc, type, type2);



                int[] conversionCount = {0};

                int[] conversionCount2 = {0};

                type = fromType.computeCommonType(toType, conversionCount);

                type2 = toType.computeCommonType(fromType, conversionCount2);

                if (conversionCount[0] == 0

                    && conversionCount2[0] == 0) {

                    assertEquals(desc, type, type2);

                }

                

                final int fromCategory = TypeUtil.typeToCategory(fromType);

                final int toCategory = TypeUtil.typeToCategory(toType);

                final int[] conversions = new int[] {0};

                final boolean canConvert =

                    TypeUtil.canConvert(

                        fromCategory,

                        toCategory,

                        conversions);

                if (canConvert && conversions[0] == 0 && type == null) {

                    if (!(fromType == memberType && toType == tupleType

                        || fromType == tupleSetType && toType == setType

                        || fromType == setType && toType == tupleSetType))

                    {

                        fail("can convert from " + fromType + " to " + toType

                            + ", but their most general type is null");

                    }

                }

                if (!canConvert && type != null && type.equals(toType)) {

                    fail("cannot convert from " + fromType + " to " + toType

                        + ", but they have a most general type " + type);

                }

            }

        }

    }

    public void testSetType(){
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure());

        Member maleChild = getMaleChild();
        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(maleChild);

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild());

        TupleType tupleType = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        SetType setTypeWithMember = new SetType(measureMemberType);
        SetType setTypeWithTuple = new SetType(tupleType);

        SetType type1 =
            (SetType) setTypeWithMember.computeCommonType(setTypeWithTuple, null);
        SetType type2 =
            (SetType) setTypeWithTuple.computeCommonType(setTypeWithMember, null);
        assertNull(type1.getDimension());
        assertNull(type1.getHierarchy());
        assertNull(type1.getLevel());
        assertEquals(1, type1.getArity());
        assertTrue(type1.usesDimension(maleChild.getDimension(),true));
        assertFalse(type1.usesDimension(maleChild.getDimension(),false));
        assertNull(type2.getDimension());
        assertNull(type2.getHierarchy());
        assertNull(type2.getLevel());
        assertEquals(1, type2.getArity());
        assertTrue(type2.usesDimension(maleChild.getDimension(),true));
        assertFalse(type2.usesDimension(maleChild.getDimension(),false));
    }

    public void testCommonTypeWhenSetTypeHavingMemberTypeAndTupleType() {
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure());

        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(getMaleChild());

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild());

        TupleType tupleType = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        SetType setTypeWithMember = new SetType(measureMemberType);
        SetType setTypeWithTuple = new SetType(tupleType);

        Type type1 = setTypeWithMember.computeCommonType(setTypeWithTuple, null);
        Type type2 = setTypeWithTuple.computeCommonType(setTypeWithMember, null);
        assertNotNull(type1);
        assertNotNull(type2);
        assertTrue(((SetType) type1).getElementType() instanceof ScalarType);
        assertTrue(((SetType) type2).getElementType() instanceof ScalarType);
        assertEquals(type1, type2);
    }

    public void testCommonTypeOfMemberandTupleTypeIsScalarType() {
        MemberType measureMemberType =
            getMemberTypeHavingMeasureInIt(getUnitSalesMeasure());

        MemberType genderMemberType =
            getMemberTypeHavingMaleChild(getMaleChild());

        MemberType storeMemberType =
            getStoreMemberType(getStoreChild());

        TupleType tupleType = new TupleType(
            new Type[] {storeMemberType, genderMemberType});

        Type type1 = measureMemberType.computeCommonType(tupleType, null);
        Type type2 = tupleType.computeCommonType(measureMemberType, null);

        assertNotNull(type1);
        assertNotNull(type2);
        assertTrue(type1 instanceof ScalarType);
        assertTrue(type2 instanceof ScalarType);
        assertEquals(type1, type2);
    }

    private MemberType getStoreMemberType(Member storeChild) {
       return new MemberType(
            storeChild.getDimension(),
            storeChild.getDimension().getHierarchy(),
            storeChild.getLevel(),
            storeChild);
    }

    private Member getStoreChild() {
        List<Id.Segment> storeParts = new ArrayList<Id.Segment>();
        storeParts.add(new Id.Segment("Store",Id.Quoting.UNQUOTED));
        storeParts.add(new Id.Segment("All Stores",Id.Quoting.UNQUOTED));
        storeParts.add(new Id.Segment("USA",Id.Quoting.UNQUOTED));
        storeParts.add(new Id.Segment("CA",Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader().getMemberByUniqueName(storeParts, false);
    }

    private MemberType getMemberTypeHavingMaleChild(Member maleChild) {
        return new MemberType(
            maleChild.getDimension(),
            maleChild.getDimension().getHierarchy(),
            maleChild.getLevel(),
            maleChild);
    }

    private MemberType getMemberTypeHavingMeasureInIt(Member unitSalesMeasure) {
        return new MemberType(
            unitSalesMeasure.getDimension(),
            unitSalesMeasure.getDimension().getHierarchy(),
            unitSalesMeasure.getDimension().getHierarchy().getLevels()[0],
            unitSalesMeasure);
    }

    private Member getMaleChild() {
        List<Id.Segment> genderParts = new ArrayList<Id.Segment>();
        genderParts.add(new Id.Segment("Gender",Id.Quoting.UNQUOTED));
        genderParts.add(new Id.Segment("M",Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader().getMemberByUniqueName(genderParts, false);
    }

    private Member getUnitSalesMeasure() {
        List<Id.Segment> measureParts = new ArrayList<Id.Segment>();
        measureParts.add(new Id.Segment("Measures",Id.Quoting.UNQUOTED));
        measureParts.add(new Id.Segment("Unit Sales",Id.Quoting.UNQUOTED));
        return getSalesCubeSchemaReader().getMemberByUniqueName(measureParts, false);
    }

    private SchemaReader getSalesCubeSchemaReader() {
        return getCubeWithName("Sales", getSchemaReader().getCubes()).
            getSchemaReader(testContext.getConnection().getRole());
    }

    private SchemaReader getSchemaReader() {
        return testContext.getConnection().getSchemaReader();
    }

    private Cube getCubeWithName(String cubeName, Cube[] cubes) {
        Cube resultCube = null;
        for (Cube cube : cubes) {
            if (cubeName.equals(cube.getName())) {
                resultCube = cube;
                break;
            }
        }
        return resultCube;
    }
}
// End TypeTest.java
