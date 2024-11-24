package libhidapi

import _root_.scala.scalanative.unsafe.*
import _root_.scala.scalanative.unsigned.*
import _root_.scala.scalanative.libc.*
import _root_.scala.scalanative.*

object predef:
  private[libhidapi] trait CEnumU[T](using eq: T =:= UInt):
    given Tag[T] = Tag.UInt.asInstanceOf[Tag[T]]
    extension (inline t: T)
     inline def int: CInt = eq.apply(t).toInt
     inline def uint: CUnsignedInt = eq.apply(t)
     inline def value: CUnsignedInt = eq.apply(t)


object enumerations:
  import predef.*
  /**
   * HID underlying bus types.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  opaque type hid_bus_type = CUnsignedInt
  object hid_bus_type extends CEnumU[hid_bus_type]:
    given _tag: Tag[hid_bus_type] = Tag.UInt
    inline def define(inline a: Long): hid_bus_type = a.toUInt
    val HID_API_BUS_UNKNOWN = define(0)
    val HID_API_BUS_USB = define(1)
    val HID_API_BUS_BLUETOOTH = define(2)
    val HID_API_BUS_I2C = define(3)
    val HID_API_BUS_SPI = define(4)
    inline def getName(inline value: hid_bus_type): Option[String] =
      inline value match
        case HID_API_BUS_UNKNOWN => Some("HID_API_BUS_UNKNOWN")
        case HID_API_BUS_USB => Some("HID_API_BUS_USB")
        case HID_API_BUS_BLUETOOTH => Some("HID_API_BUS_BLUETOOTH")
        case HID_API_BUS_I2C => Some("HID_API_BUS_I2C")
        case HID_API_BUS_SPI => Some("HID_API_BUS_SPI")
        case _ => None
    extension (a: hid_bus_type)
      inline def &(b: hid_bus_type): hid_bus_type = a & b
      inline def |(b: hid_bus_type): hid_bus_type = a | b
      inline def is(b: hid_bus_type): Boolean = (a & b) == b

object aliases:
  import _root_.libhidapi.enumerations.*
  import _root_.libhidapi.predef.*
  import _root_.libhidapi.aliases.*
  import _root_.libhidapi.structs.*
  type size_t = libc.stddef.size_t
  object size_t:
    val _tag: Tag[size_t] = summon[Tag[libc.stddef.size_t]]
    inline def apply(inline o: libc.stddef.size_t): size_t = o
    extension (v: size_t)
      inline def value: libc.stddef.size_t = v

  type wchar_t = libc.stddef.wchar_t
  object wchar_t:
    val _tag: Tag[wchar_t] = summon[Tag[libc.stddef.wchar_t]]
    inline def apply(inline o: libc.stddef.wchar_t): wchar_t = o
    extension (v: wchar_t)
      inline def value: libc.stddef.wchar_t = v

object structs:
  import _root_.libhidapi.enumerations.*
  import _root_.libhidapi.predef.*
  import _root_.libhidapi.aliases.*
  import _root_.libhidapi.structs.*
  /**
   * A structure to hold the version numbers.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  opaque type hid_api_version = CStruct3[CInt, CInt, CInt]
  object hid_api_version:
    given _tag: Tag[hid_api_version] = Tag.materializeCStruct3Tag[CInt, CInt, CInt]
    def apply()(using Zone): Ptr[hid_api_version] = scala.scalanative.unsafe.alloc[hid_api_version](1)
    def apply(major : CInt, minor : CInt, patch : CInt)(using Zone): Ptr[hid_api_version] =
      val ____ptr = apply()
      (!____ptr).major = major
      (!____ptr).minor = minor
      (!____ptr).patch = patch
      ____ptr
    extension (struct: hid_api_version)
      def major : CInt = struct._1
      def major_=(value: CInt): Unit = !struct.at1 = value
      def minor : CInt = struct._2
      def minor_=(value: CInt): Unit = !struct.at2 = value
      def patch : CInt = struct._3
      def patch_=(value: CInt): Unit = !struct.at3 = value

  /**
   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  opaque type hid_device = CStruct0
  object hid_device:
    given _tag: Tag[hid_device] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  opaque type hid_device_ = CStruct0
  object hid_device_ :
    given _tag: Tag[hid_device_] = Tag.materializeCStruct0Tag

  /**
   * hidapi info structure

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  opaque type hid_device_info = CStruct12[CString, CUnsignedShort, CUnsignedShort, Ptr[wchar_t], CUnsignedShort, Ptr[wchar_t], Ptr[wchar_t], CUnsignedShort, CUnsignedShort, CInt, Ptr[Byte], hid_bus_type]
  object hid_device_info:
    given _tag: Tag[hid_device_info] = Tag.materializeCStruct12Tag[CString, CUnsignedShort, CUnsignedShort, Ptr[wchar_t], CUnsignedShort, Ptr[wchar_t], Ptr[wchar_t], CUnsignedShort, CUnsignedShort, CInt, Ptr[Byte], hid_bus_type]
    def apply()(using Zone): Ptr[hid_device_info] = scala.scalanative.unsafe.alloc[hid_device_info](1)
    def apply(path : CString, vendor_id : CUnsignedShort, product_id : CUnsignedShort, serial_number : Ptr[wchar_t], release_number : CUnsignedShort, manufacturer_string : Ptr[wchar_t], product_string : Ptr[wchar_t], usage_page : CUnsignedShort, usage : CUnsignedShort, interface_number : CInt, next : Ptr[hid_device_info], bus_type : hid_bus_type)(using Zone): Ptr[hid_device_info] =
      val ____ptr = apply()
      (!____ptr).path = path
      (!____ptr).vendor_id = vendor_id
      (!____ptr).product_id = product_id
      (!____ptr).serial_number = serial_number
      (!____ptr).release_number = release_number
      (!____ptr).manufacturer_string = manufacturer_string
      (!____ptr).product_string = product_string
      (!____ptr).usage_page = usage_page
      (!____ptr).usage = usage
      (!____ptr).interface_number = interface_number
      (!____ptr).next = next
      (!____ptr).bus_type = bus_type
      ____ptr
    extension (struct: hid_device_info)
      def path : CString = struct._1
      def path_=(value: CString): Unit = !struct.at1 = value
      def vendor_id : CUnsignedShort = struct._2
      def vendor_id_=(value: CUnsignedShort): Unit = !struct.at2 = value
      def product_id : CUnsignedShort = struct._3
      def product_id_=(value: CUnsignedShort): Unit = !struct.at3 = value
      def serial_number : Ptr[wchar_t] = struct._4
      def serial_number_=(value: Ptr[wchar_t]): Unit = !struct.at4 = value
      def release_number : CUnsignedShort = struct._5
      def release_number_=(value: CUnsignedShort): Unit = !struct.at5 = value
      def manufacturer_string : Ptr[wchar_t] = struct._6
      def manufacturer_string_=(value: Ptr[wchar_t]): Unit = !struct.at6 = value
      def product_string : Ptr[wchar_t] = struct._7
      def product_string_=(value: Ptr[wchar_t]): Unit = !struct.at7 = value
      def usage_page : CUnsignedShort = struct._8
      def usage_page_=(value: CUnsignedShort): Unit = !struct.at8 = value
      def usage : CUnsignedShort = struct._9
      def usage_=(value: CUnsignedShort): Unit = !struct.at9 = value
      def interface_number : CInt = struct._10
      def interface_number_=(value: CInt): Unit = !struct.at10 = value
      def next : Ptr[hid_device_info] = struct._11.asInstanceOf[Ptr[hid_device_info]]
      def next_=(value: Ptr[hid_device_info]): Unit = !struct.at11 = value.asInstanceOf[Ptr[Byte]]
      def bus_type : hid_bus_type = struct._12
      def bus_type_=(value: hid_bus_type): Unit = !struct.at12 = value

// @link("hidapi")
// TODO
@extern
private[libhidapi] object extern_functions:
  import _root_.libhidapi.enumerations.*
  import _root_.libhidapi.predef.*
  import _root_.libhidapi.aliases.*
  import _root_.libhidapi.structs.*
  /**
   * Close a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_close(dev : Ptr[hid_device]): Unit = extern

  /**
   * Enumerate the HID Devices.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_enumerate(vendor_id : CUnsignedShort, product_id : CUnsignedShort): Ptr[hid_device_info] = extern

  /**
   * Get a string describing the last error which occurred.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_error(dev : Ptr[hid_device]): Ptr[wchar_t] = extern

  /**
   * Finalize the HIDAPI library.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_exit(): CInt = extern

  /**
   * Free an enumeration Linked List

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_free_enumeration(devs : Ptr[hid_device_info]): Unit = extern

  /**
   * Get The struct #hid_device_info from a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_get_device_info(dev : Ptr[hid_device]): Ptr[hid_device_info] = extern

  /**
   * Get a feature report from a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_get_feature_report(dev : Ptr[hid_device], data : Ptr[CUnsignedChar], length : size_t): CInt = extern

  /**
   * Get a string from a HID device, based on its string index.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_get_indexed_string(dev : Ptr[hid_device], string_index : CInt, string : Ptr[wchar_t], maxlen : size_t): CInt = extern

  /**
   * Get a input report from a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_get_input_report(dev : Ptr[hid_device], data : Ptr[CUnsignedChar], length : size_t): CInt = extern

  /**
   * Get The Manufacturer String from a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_get_manufacturer_string(dev : Ptr[hid_device], string : Ptr[wchar_t], maxlen : size_t): CInt = extern

  /**
   * Get The Product String from a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_get_product_string(dev : Ptr[hid_device], string : Ptr[wchar_t], maxlen : size_t): CInt = extern

  /**
   * Get a report descriptor from a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_get_report_descriptor(dev : Ptr[hid_device], buf : Ptr[CUnsignedChar], buf_size : size_t): CInt = extern

  /**
   * Get The Serial Number String from a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_get_serial_number_string(dev : Ptr[hid_device], string : Ptr[wchar_t], maxlen : size_t): CInt = extern

  /**
   * Initialize the HIDAPI library.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_init(): CInt = extern

  /**
   * Open a HID device using a Vendor ID (VID), Product ID (PID) and optionally a serial number.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_open(vendor_id : CUnsignedShort, product_id : CUnsignedShort, serial_number : Ptr[wchar_t]): Ptr[hid_device] = extern

  /**
   * Open a HID device by its path name.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_open_path(path : CString): Ptr[hid_device] = extern

  /**
   * Read an Input report from a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_read(dev : Ptr[hid_device], data : Ptr[CUnsignedChar], length : size_t): CInt = extern

  /**
   * Read an Input report from a HID device with timeout.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_read_timeout(dev : Ptr[hid_device], data : Ptr[CUnsignedChar], length : size_t, milliseconds : CInt): CInt = extern

  /**
   * Send a Feature report to the device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_send_feature_report(dev : Ptr[hid_device], data : Ptr[CUnsignedChar], length : size_t): CInt = extern

  /**
   * Set the device handle to be non-blocking.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_set_nonblocking(dev : Ptr[hid_device], nonblock : CInt): CInt = extern

  /**
   * Get a runtime version of the library.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_version(): Ptr[hid_api_version] = extern

  /**
   * Get a runtime version string of the library.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_version_str(): CString = extern

  /**
   * Write an Output report to a HID device.

   * [bindgen] header: /nix/store/l0wpm81l7qdjab8bhvs8ip78gdqg3w61-hidapi-0.14.0/include/hidapi/hidapi.h
  */
  def hid_write(dev : Ptr[hid_device], data : Ptr[CUnsignedChar], length : size_t): CInt = extern


object functions:
  import _root_.libhidapi.enumerations.*
  import _root_.libhidapi.predef.*
  import _root_.libhidapi.aliases.*
  import _root_.libhidapi.structs.*
  import extern_functions.*
  export extern_functions.*

object types:
  export _root_.libhidapi.structs.*
  export _root_.libhidapi.aliases.*
  export _root_.libhidapi.enumerations.*

object all:
  export _root_.libhidapi.enumerations.hid_bus_type
  export _root_.libhidapi.aliases.size_t
  export _root_.libhidapi.aliases.wchar_t
  export _root_.libhidapi.structs.hid_api_version
  export _root_.libhidapi.structs.hid_device
  export _root_.libhidapi.structs.hid_device_
  export _root_.libhidapi.structs.hid_device_info
  export _root_.libhidapi.functions.hid_close
  export _root_.libhidapi.functions.hid_enumerate
  export _root_.libhidapi.functions.hid_error
  export _root_.libhidapi.functions.hid_exit
  export _root_.libhidapi.functions.hid_free_enumeration
  export _root_.libhidapi.functions.hid_get_device_info
  export _root_.libhidapi.functions.hid_get_feature_report
  export _root_.libhidapi.functions.hid_get_indexed_string
  export _root_.libhidapi.functions.hid_get_input_report
  export _root_.libhidapi.functions.hid_get_manufacturer_string
  export _root_.libhidapi.functions.hid_get_product_string
  export _root_.libhidapi.functions.hid_get_report_descriptor
  export _root_.libhidapi.functions.hid_get_serial_number_string
  export _root_.libhidapi.functions.hid_init
  export _root_.libhidapi.functions.hid_open
  export _root_.libhidapi.functions.hid_open_path
  export _root_.libhidapi.functions.hid_read
  export _root_.libhidapi.functions.hid_read_timeout
  export _root_.libhidapi.functions.hid_send_feature_report
  export _root_.libhidapi.functions.hid_set_nonblocking
  export _root_.libhidapi.functions.hid_version
  export _root_.libhidapi.functions.hid_version_str
  export _root_.libhidapi.functions.hid_write
