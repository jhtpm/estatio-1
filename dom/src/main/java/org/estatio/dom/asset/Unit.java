package org.estatio.dom.asset;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Element;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.Join;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Version;
import javax.jdo.annotations.VersionStrategy;

import org.estatio.dom.communicationchannel.CommunicationChannel;
import org.estatio.dom.communicationchannel.CommunicationChannelType;
import org.estatio.dom.lease.LeaseUnit;

import org.apache.isis.applib.annotation.AutoComplete;
import org.apache.isis.applib.annotation.Disabled;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.MemberOrder;
import org.apache.isis.applib.annotation.PublishedObject;
import org.apache.isis.applib.annotation.Render;
import org.apache.isis.applib.annotation.Render.Type;
import org.apache.isis.applib.annotation.Where;

@PersistenceCapable
@Version(strategy = VersionStrategy.VERSION_NUMBER, column = "VERSION")
@Inheritance(strategy = InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
@AutoComplete(repository = Units.class)
@PublishedObject
public class Unit extends FixedAsset {

    // {{ Type (attribute)
    private UnitType unitType;

    @MemberOrder(sequence = "3")
    public UnitType getUnitType() {
        return unitType;
    }

    public void setUnitType(final UnitType type) {
        this.unitType = type;
    }

    public List<UnitType> choicesUnitType() {
        return Arrays.asList(UnitType.values());
    }

    // }}

    // {{ Area (attribute)
    private BigDecimal area;

    @MemberOrder(sequence = "4")
    @Column(scale = 2)
    public BigDecimal getArea() {
        return area;
    }

    public void setArea(final BigDecimal area) {
        this.area = area;
    }

    // }}

    // {{ StorageArea (property)
    private BigDecimal storageArea;

    @Hidden(where = Where.PARENTED_TABLES)
    @MemberOrder(sequence = "5")
    @Column(scale = 2)
    public BigDecimal getStorageArea() {
        return storageArea;
    }

    public void setStorageArea(final BigDecimal storageArea) {
        this.storageArea = storageArea;
    }

    // }}

    // {{ SalesArea (property)
    private BigDecimal salesArea;

    @Hidden(where = Where.PARENTED_TABLES)
    @MemberOrder(sequence = "6")
    @Column(scale = 2)
    public BigDecimal getSalesArea() {
        return salesArea;
    }

    public void setSalesArea(final BigDecimal salesArea) {
        this.salesArea = salesArea;
    }

    // }}

    // {{ MezzanineArea (property)
    private BigDecimal mezzanineArea;

    @Hidden(where = Where.PARENTED_TABLES)
    @MemberOrder(sequence = "7")
    @Column(scale = 2)
    public BigDecimal getMezzanineArea() {
        return mezzanineArea;
    }

    public void setMezzanineArea(final BigDecimal mezzanineArea) {
        this.mezzanineArea = mezzanineArea;
    }

    // }}

    // {{ TerraceArea (property)
    private BigDecimal terraceArea;

    @Hidden(where = Where.PARENTED_TABLES)
    @MemberOrder(sequence = "8")
    @Column(scale = 2)
    public BigDecimal getTerraceArea() {
        return terraceArea;
    }

    public void setTerraceArea(final BigDecimal terraceArea) {
        this.terraceArea = terraceArea;
    }

    // }}

    // {{ Property (attribute)
    private Property property;

    @javax.jdo.annotations.Column(name = "PROPERTY_ID")
    @Hidden(where = Where.PARENTED_TABLES)
    @Disabled
    @MemberOrder(sequence = "9")
    public Property getProperty() {
        return property;
    }

    public void setProperty(final Property property) {
        this.property = property;
    }

    // }}

    // {{ Numerators (Collection)
    @Persistent(mappedBy = "unit", defaultFetchGroup = "false")
    private SortedSet<LeaseUnit> leases = new TreeSet<LeaseUnit>();

    @Render(Type.EAGERLY)
    @MemberOrder(sequence = "2.2")
    public SortedSet<LeaseUnit> getLeases() {
        return leases;
    }

    public void setLeases(final SortedSet<LeaseUnit> leases) {
        this.leases = leases;
    }

    public void addToLeases(final LeaseUnit leaseUnit) {
        // check for no-op
        if (leaseUnit == null || getLeases().contains(leaseUnit)) {
            return;
        }
        // associate new
        getLeases().add(leaseUnit);
    }

    public void removeFromLeases(final LeaseUnit leaseUnit) {
        // check for no-op
        if (leaseUnit == null || !getLeases().contains(leaseUnit)) {
            return;
        }
        // dissociate existing
        getLeases().remove(leaseUnit);
    }

}
