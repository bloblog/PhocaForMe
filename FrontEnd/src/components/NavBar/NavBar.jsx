import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";

import { logoutUser } from "../../store2/loginUser.js";

import Bell from "./Bell";
import ProfileImage from "./ProfileImage";

import logo from "../../assets/images/logo.PNG";
import NonLoginImage from "./NonLoginImage.jsx";

const NavBar = ({ children }) => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const user = useSelector((state) => state.user.user);

  const handleLogout = () => {
    dispatch(logoutUser());
    console.log(user);
    navigate("/main");
  };

  return (
    <div id="nav-bar">
      <img
        id="logo"
        onClick={() => {
          navigate("/main");
        }}
        src={logo}
      />

      {user ? (
        <div id="nav-menus">
          <ProfileImage />
          {/* <button onClick={handleLogout}>로그아웃</button> */}
        </div>
      ) : (
        // 여기가 로그아웃상태에서 보이는 거
        <div>
          <NonLoginImage />
        </div>
      )}

      <div
        id="bell"
        onClick={() => {
          navigate("/alarm");
        }}
      >
        <Bell />
      </div>
    </div>
  );
};
export default NavBar;
